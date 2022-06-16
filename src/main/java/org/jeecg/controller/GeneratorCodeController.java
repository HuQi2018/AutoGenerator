package org.jeecg.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.Result;
import org.jeecg.dto.GeneratorCodeDTO;
import org.jeecg.entity.FieldEntity;
import org.jeecg.entity.GeneratorCodeDO;
import org.jeecg.service.GeneratorCodeService;
import org.jeecg.util.FileUtil;
import org.jeecg.util.ServiceUtils;
import org.jeecg.util.oConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.jeecg.constant.GeneratorCodeConstant.*;
import static org.jeecg.util.GeneratorCodeUtil.*;

/**
 * 代码生成器：添加代码生成器、移除代码生成器、清空代码生成器（所有）
 *
 * @author: HuQi
 * @date: 2021年07月13日 15:09
 */
@RequestMapping(value = "/generator")
@RestController
@Api(tags = "表单代码生成器")
@Slf4j
public class GeneratorCodeController {

    @Autowired
    private GeneratorCodeService generatorCodeService;

    private ResourceBundle setting;

    /**
     * 添加代码生成器，用户通过输入表名以及表所有的相关字段，和代码生成器所需要的模板ID，同时也可设定只生成部分内容
     * 模板ID可不设定，默认使用默认模板。表名需具备唯一性，存在了则无法再创建。若只生成部分内容，
     * 需注意前后部分内容是否有关联
     *
     * @param tableName 数据库表名
     * @param json 表的相关字段信息（字段名，类型，备注，长度 ），JSON格式。
     *             如：[
     *           {
     *                     "name":"字段名1",
     *                     "type":"类型1",
     *                     "mark":"字段备注1",
     *                     "length":"字段长度1"
     *           },
     *           {
     *                     "name":"字段名2",
     *                     "type":"类型2",
     *                     "mark":"字段备注2",
     *                     "length":"字段长度2"
     *           }
     * ]
     * @param templateId 模板ID
     * @param isAutoTable 是否自动生成表
     * @param isAutoCode 是否自动生成表
     * @param isAutoCompile 是否自动生成代码
     * @param isAutoLoad 是否自动编译
     * @param updateFieldsJson 需要更新的字段信息 ，JSON格式
     *             如：{"oldName":{"name":"NEWname","type":"text","mark":"新姓名","length":"32"},{...}}
     * @return org.jeecg.common.api.vo.Result
     * @Author HuQi、LiKun
     * @create 2021-08-02 17:08
     */
    @ApiOperation(value = "添加代码生成器")
    @PostMapping(value = "/addGenerator")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> autoGenerator(@ApiParam(value = "数据库表名", required = true) @RequestParam String tableName,
                                        @ApiParam(value = "表的相关字段和注释，JSON格式，如：[{\"name\":\"name\",\"type\":" +
                                                "\"varchar\",\"mark\":\"姓名\",\"length\":\"255\"},{\"name\":\"age\"," +
                                                "\"type\":\"int\",\"mark\":\"年龄\",\"length\":\"3\"}]", required = true)
                                            @RequestParam String json,
                                        @ApiParam(value = "需要更新的字段,如：{\"age\":{\"name\":\"new_age\"," +
                                                "\"type\":\"new_type\",\"mark\":\"new_mark\",\"length\":\"new_length\"}}")
                                            @RequestParam String updateFieldsJson,
                                        @ApiParam(value = "模板ID") @RequestParam(required = false) String templateId,
                                        @ApiParam(value = "是否自动生成表（包括对表的修改）") @RequestParam(defaultValue = "1") Integer isAutoTable,
                                        @ApiParam(value = "是否自动生成代码") @RequestParam(defaultValue = "1") Integer isAutoCode,
                                        @ApiParam(value = "是否自动编译") @RequestParam(defaultValue = "1") Integer isAutoCompile,
                                        @ApiParam(value = "是否自动加载") @RequestParam(defaultValue = "1") Integer isAutoLoad)
    {
        GeneratorCodeDTO generatorCodeVO = new GeneratorCodeDTO();
        GeneratorCodeDO generatorTable = generatorCodeService.hasGeneratorTable(tableName);
        setting = generatorCodeVO.getGeneratorSetting();
        String tablePrefix = setting.getString(SETTING_TABLE_PREFIX);
        String tableNamePlus = tablePrefix + tableName;

        // 使用表名作为模块名，同时也是代码的上一文件夹名，方便管理
        generatorCodeVO.setModuleName(oConvertUtils.camelNameCapFirst(tableName));
        generatorCodeVO.setTableName(tableNamePlus);
        generatorCodeVO.setUserName(getUser().getUsername());

        if (generatorTable != null && !generatorTable.getCreateBy().equals(generatorCodeVO.getUserName())){
            return Result.Error(MESSAGE_ERROR_TABLE_ALREADY_USE);
        }

        StringBuilder res = new StringBuilder(MESSAGE_INFO_GENERATOR_START);

        // 1、创建创建表或更新表
        if (isAutoTable == 1) {
            generatorTable = createAutoTable(json, generatorTable, tablePrefix, tableName, tableNamePlus, updateFieldsJson, res);
            if (generatorTable == null){
                return Result.OK(tableName + MESSAGE_INFO_FIELD_NOT_CHANGE);
            }
        }

        // 2、创建业务层代码，前提表存在
        if (isAutoCode == 1) {
            createAutoCode(generatorTable, generatorCodeVO, templateId, res);
        }

        // 3、动态编译业务层代码并打包，无条件，重新打包文件夹内所有文件
        if (isAutoCompile == 1) {
            createAutoCompile(tableName, generatorCodeVO, res);
        }

        // 4、动态加载代码，jar包内需要相关编译后的class文件
        if (isAutoLoad == 1) {
            createAutoLoad(generatorTable, tableName, generatorCodeVO, res);
        }
        return Result.OK(res + MESSAGE_SUCCESS_GENERATOR);
    }

    /**
     * 1、创建创建表或更新表
     *
     * @param json 表的相关字段信息（字段名，类型，备注，长度 ），JSON格式。
     *             如：[
     *           {
     *                     "name":"字段名1",
     *                     "type":"类型1",
     *                     "mark":"字段备注1",
     *                     "length":"字段长度1"
     *           },
     *           {
     *                     "name":"字段名2",
     *                     "type":"类型2",
     *                     "mark":"字段备注2",
     *                     "length":"字段长度2"
     *           }
     * ]
     * @param generatorTable 代码生成器表实体类
     * @param tablePrefix 表前缀
     * @param tableName 表名
     * @param tableNamePlus 加了前缀后的表名
     * @param updateFieldsJson 需要更新的字段信息 ，JSON格式
     *             如：{"oldName":{"name":"NEWname","type":"text","mark":"新姓名","length":"32"},{...}}
     * @param res 结果
     * @return org.jeecg.modules.form.entity.GeneratorCodeDO
     * @Author HuQi、LiKun
     * @create 2021-08-27 14:15
     */
    private GeneratorCodeDO createAutoTable(String json, GeneratorCodeDO generatorTable, String tablePrefix, String tableName,
                                            String tableNamePlus, String updateFieldsJson, StringBuilder res) {
        // 表已经存在，增加，删除或更新字段
        log.info(MESSAGE_INFO_URL_PARSE);
        String result = "";
        try {
            result = java.net.URLDecoder.decode(json, "utf8");
        } catch (UnsupportedEncodingException e) {
            log.error(MESSAGE_ERROR_JSON_PARSE + e.getMessage());
        }
        log.info(MESSAGE_SUCCESS_URL_PARSE);
        JSONArray jsonArray = JSONArray.parseArray(result);
        List<String> nameToList = new ArrayList<>();
        List<String> typeToList = new ArrayList<>();
        List<Integer> lengthToList = new ArrayList<>();
        List<String> markToList = new ArrayList<>();
        Map<String,Object> fieldsInfoMap = new HashMap<>();
        // 传入的所有字段名
        StringBuilder nameToString= new StringBuilder();
        // 按照名称，类型，长度，备注将每个字段信息存入List
        for(int i=0;i<jsonArray.size();i++){
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            FieldEntity fieldEntity = JSONObject.parseObject(jsonObject.toString(), FieldEntity.class);
            if(!("").equals(fieldEntity.getName())){
                nameToList.add(fieldEntity.getName());
                nameToString.append(setting.getString(SETTING_TABLE_PREFIX)).append(fieldEntity.getName()).append(",");
            }else {
                log.error(MESSAGE_ERROR_NAME_IS_NULL);
                ServiceUtils.throwException(MESSAGE_ERROR_NAME_IS_NULL);
            }
            if(!("").equals(fieldEntity.getType())){
                typeToList.add(fieldEntity.getType());
            }else {
                log.error(fieldEntity.getName()+MESSAGE_ERROR_TYPE_IS_NULL);
                ServiceUtils.throwException(fieldEntity.getName()+MESSAGE_ERROR_TYPE_IS_NULL);
            }
            lengthToList.add(fieldEntity.getLength());
            markToList.add(fieldEntity.getMark());
            // 将字段信息按照{"oldName":{"name":"oldName","type":"text","mark":"新姓名","length":"32"},{...}}格式存储
            // 为更新字段时判断字段内容是否发生变化做准备
            Map<String,Object> field = jsonToMap(jsonObject.toString());
            fieldsInfoMap.put((String) field.get("name"),field);
        }
        JSONObject fieldsInfo = new JSONObject(fieldsInfoMap);

        // Map<String, Object> jsonToMap = jsonToMap(result);
        // Map<String, Object> jsonToMap = null;
        // String keyToString = mapKeyToString(setting.getString(SETTING_TABLE_PREFIX), jsonToMap);
        // String keyToString = null;
        // List<String> keyToList = mapKeyToList(jsonToMap);
        // List<Object> valueToList = mapValueToList(jsonToMap);

        // 更新
        if (generatorTable != null) {
            // 判断字段信息是否被更改，若无更改，则直接返回，结束请求
            Boolean flag = true;
            Map<String,Object> oldFieldsInfo = jsonToMap(generatorTable.getFieldsInfo());
            Map<String,Object> updateFieldsInfo = jsonToMap(updateFieldsJson);
            if(Objects.isNull(updateFieldsInfo)){
                ServiceUtils.throwException("请给出更新字段的更新内容，填入updateFieldsJson！");
            }
            for(Map.Entry<String, Object> updateFieldInfo : updateFieldsInfo.entrySet()){
                if(oldFieldsInfo.get(updateFieldInfo.getKey())==null||("").equals(oldFieldsInfo.get(updateFieldInfo.getKey()))){
                    ServiceUtils.throwException("更新的" + updateFieldInfo.getKey() + "字段不存在！");
                }
                if(("").equals(updateFieldInfo.getValue())){
                    ServiceUtils.throwException("更新的" + updateFieldInfo.getKey() + "信息不存在！");
                }
                Map<String,String> updateInfo = (Map<String, String>) updateFieldInfo.getValue();
                Map<String,String> oldInfo = (Map)oldFieldsInfo.get(updateFieldInfo.getKey());

                for(Map.Entry<String, String> info : updateInfo.entrySet()){
                    if(!info.getValue().equals(oldInfo.get(info.getKey()))){
                        flag = false;
                        break;
                    }
                }
                // 判断更新updateFieldsJson中的字段信息是否和包含所有字段信息json中一致
                for(int i = 0; i<jsonArray.size();i++){
                    Map<String,Object> newFieldInfo = jsonToMap(jsonArray.getJSONObject(i).toString());
                    if(newFieldInfo.get("name").equals(updateInfo.get("name"))){
                        if(!newFieldInfo.get("type").equals(updateInfo.get("type"))||
                                !newFieldInfo.get("mark").equals(updateInfo.get("mark"))||
                                !newFieldInfo.get("length").equals(updateInfo.get("length"))){
                            log.error(MESSAGE_ERROR_UPDATE_JSON_IS_DIFFER);
                            ServiceUtils.throwException(newFieldInfo.get("name")+MESSAGE_ERROR_UPDATE_JSON_IS_DIFFER);
                        }
                    }
                }
            }
            if (flag){
                log.info(tableName + MESSAGE_INFO_FIELD_NOT_CHANGE);
                return null;
            }
            // 取传入的所有字段名
            String[] newFields = nameToString.toString().split(FILE_SPLIT_STR);
            List<String> commonList = new ArrayList<>(Arrays.asList(newFields));
            List<String> addList = new ArrayList<>(Arrays.asList(newFields));
            // 取旧字段名
            List<String> oldList = new ArrayList<>();
            for(String key : oldFieldsInfo.keySet()){
                oldList.add(tablePrefix+key);
            }
            // String[] oldFields = generatorTable.getFields().split(FILE_SPLIT_STR);
            // List<String> oldList = new ArrayList<>(Arrays.asList(oldFields));

            if (!"".equals(updateFieldsJson)) {
                // 更新字段
                updateFields( updateFieldsJson, tablePrefix, tableName, addList, commonList, oldList);
            }
            // 取交集，即剩下不做改变的字段名
            commonList.retainAll(oldList);
            // 新增加
            addList.removeAll(commonList);
            if (!addList.isEmpty()) {
                List<Map<String,Object>> list = new ArrayList<>();
                // 从所有新字段信息中匹配需要新增的字段信息
                for(int i = 0; i < addList.size(); i++){
                    for(int j=0;j<jsonArray.size();j++){
                        Map<String,Object> newFieldInfo = jsonToMap(jsonArray.getJSONObject(j).toString());
                        if(addList.get(i).replace(tablePrefix,"").equals(newFieldInfo.get("name"))){
                            newFieldInfo.remove("name");
                            // 保存新增字段的信息
                            list.add(newFieldInfo);
                            break;
                        }
                    }
                }
                try {
                    generatorCodeService.addFields(addList, list, tablePrefix + tableName);
                } catch (Exception e) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    log.error(e.getMessage());
                }
            }
            // 旧删除
            oldList.removeAll(commonList);
            if (!oldList.isEmpty()) {
                // 删除表中字段
                try {
                    generatorCodeService.deleteFields(oldList, tablePrefix + tableName);
                } catch (Exception e) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    log.error(e.getMessage());
                }
            }
            // 更新generator_code表中的fields信息
            // generatorTable.setFields(nameToString.toString());
            generatorTable.setFieldsInfo(fieldsInfo.toString());
            generatorCodeService.updateGeneratorTable(generatorTable);

            if (TRUE_STR.equals(setting.getString(SETTING_IS_PERSISTENT)) &&
                    SETTING_PERSISTENT_METHOD_GITLAB.equals(setting.getString(SETTING_PERSISTENT_METHOD))) {
                // 开启线程删除目录
                String path = setting.getString(SETTING_GITLAB_TREE) + "/" + oConvertUtils.camelNameCapFirst(tableName);
                new Thread(() -> generatorCodeService.deletePathFromGitLab(path, setting)).start();
            }
            removeGenerator(tableName, 0, 0, 1);
        } else {
            // 新建表
            generatorTable = createTable(tableName, tableNamePlus, tablePrefix, nameToList, markToList,lengthToList
                    ,typeToList, res,fieldsInfo.toString());
        }
        return generatorTable;
    }

    /**
     * 创建表，当表不存在时创建
     *
     * @param tableName 表名
     * @param tableNamePlus 含有前缀的表名
     * @param tablePrefix 前缀
     * @param nameToList 所有字段名
     * @param markToList 所有字段备注
     * @param res 结果输出字符串
     * @return org.jeecg.modules.form.entity.GeneratorCodeDO
     * @Author HuQi、LiKun
     * @create 2021-08-27 14:20
     */
    private GeneratorCodeDO createTable(String tableName, String tableNamePlus, String tablePrefix,
                                        List<String> nameToList, List<String> markToList, List<Integer> lengthToList
            , List<String> typeToList, StringBuilder res,String fieldsInfo) {
        // 表不存在，新生成表
        GeneratorCodeDO generatorTable = new GeneratorCodeDO();
        generatorTable.setTableName(tableName);
        if (nameToList.isEmpty()) {
            ServiceUtils.throwException(MESSAGE_ERROR_FIELD_NOT_EXIST);
        }
        try {
            log.info(tableName + MESSAGE_INFO_GENERATOR_TABLE);
            tableNamePlus = generatorCodeService.createTable(tablePrefix, tableNamePlus, nameToList, markToList,lengthToList,typeToList);
            generatorTable.setTableNamePlus(tableNamePlus);
            generatorTable.setIsEnableUrl(1);
            // 更新generator_code表中的fields信息
            // generatorTable.setFields(keyToString);
            generatorTable.setFieldsInfo(fieldsInfo);
            generatorCodeService.addGenertatorTable(generatorTable);
            log.info(MESSAGE_SUCCESS_GENERATOR_TABLE + tableNamePlus);
            res.append(MESSAGE_SUCCESS_GENERATOR_TABLE).append(tableNamePlus).append("    ");
        } catch (Exception e) {
            log.error(MESSAGE_ERROR_GENERATOR_TABLE + e.getMessage());
            ServiceUtils.throwException(res + e.getMessage() + MESSAGE_ERROR_GENERATOR_TABLE);
        }
        return generatorTable;
    }

    /**
     * 更新字段，修改旧字段名
     *
     * @param updateFieldsJson 更新字段的json信息
     * @param tablePrefix 表名前缀
     * @param tableName 表名
     * @param addList 新增字段的列表
     * @param commonList 共同字段列表
     * @param oldList 旧字段的列表
     * @return void
     * @Author HuQi、LiKun
     * @create 2021-08-27 17:24
     */
    private void updateFields( String updateFieldsJson, String tablePrefix,
                              String tableName, List<String> addList, List<String> commonList, List<String> oldList) {
        log.info(MESSAGE_INFO_UPDATE_URL_PARSE);
        String str = "";
        try {
            str = java.net.URLDecoder.decode(updateFieldsJson, "utf8");
        } catch (UnsupportedEncodingException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            ServiceUtils.throwException(MESSAGE_ERROR_UPDATE_JSON_PARSE);
        }
        log.info(MESSAGE_SUCCESS_UPDATE_URL_PARSE);

        // List<String> oldFieldsList = new ArrayList<>();
            Map<String, Object> updateFieldsJsonToMap = jsonToMap(str);
            // if(updateFieldsJsonToMap.isEmpty()){
            //     log.error(MESSAGE_ERROR_UPDATE_JSON_IS_NULL);
            //     ServiceUtils.throwException(MESSAGE_ERROR_UPDATE_JSON_IS_NULL);
            // }
            // 判断更新的字段是否存在
            for (Map.Entry<String, Object> m : updateFieldsJsonToMap.entrySet()) {
                // 存储所有需要更新字段的旧字段名
                // oldFieldsList.add(tablePrefix + m.getKey());
                // 将仅作更新操作的字段名从所有旧字段名中删除，即留下添加，删除以及不做改动的字段名，用作判断更新的字段名是否与原有字段名相同的情况
                oldList.remove(tablePrefix + m.getKey());
            }
        // 从更新json中取key和存储的旧key比较，查看传入的需要更新字段是否存在
        // oldFieldsList.removeAll(Arrays.asList(generatorTable.getFields().split(FILE_SPLIT_STR)));
        // if (!oldFieldsList.isEmpty()) {
        //     ServiceUtils.throwException("更新的" + oldFieldsList.toString().replace(tablePrefix, "") + "字段不存在！");
        // }

        try {
            // 返回值包含所有更新字段的字段名
            List<String> rsList = generatorCodeService.updateFields(str, tableName, tablePrefix, oldList);
            // 将仅作修改操作的字段名从传入的所有字段名移中移除，即只剩下添加，删除以及不做改动的字段名
            commonList.removeAll(rsList);
            addList.removeAll(rsList);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            ServiceUtils.throwException(e.getMessage());
        }
    }

    /**
     * 2、创建业务层代码，前提表存在
     * 根据生成的数据库表生成相应的代码文件
     * 若有自定义模板，则根据自定义模板生成，若无模板，则根据默认模板生成
     *
     * @param generatorTable 代码生成器表实体类
     * @param generatorCodeVO 业务层交互数据传输类
     * @param templateId 模板ID
     * @param res 结果输出字符串
     * @return void
     * @Author HuQi
     * @create 2021-08-27 17:26
     */
    @Transactional(rollbackFor = Exception.class)
    public void createAutoCode(GeneratorCodeDO generatorTable, GeneratorCodeDTO generatorCodeVO, String templateId, StringBuilder res) {
        if (generatorTable == null){
            ServiceUtils.throwException(generatorCodeVO.getTableName() + MESSAGE_ERROR_TABLE_NOT_EXIST);
            return;
        }
        try {
            String projectPath = generatorCodeService.generatorCode(generatorCodeVO, templateId);
            generatorTable.setIsGeneratorCode(1);
            generatorTable.setCodePath(projectPath);
            generatorTable.setTemplateId(templateId);
            generatorCodeService.updateGeneratorTable(generatorTable);
            log.info(MESSAGE_SUCCESS_GENERATOR_CODE + projectPath);
            res.append(MESSAGE_SUCCESS_GENERATOR_CODE).append(projectPath).append("    ");
        } catch (Exception e) {
            log.error(generatorCodeVO.getTableName() + MESSAGE_ERROR_GENERATOR_CODE + e.getMessage());
            ServiceUtils.throwException(res + e.getMessage() + MESSAGE_ERROR_GENERATOR_CODE);
        }
    }

    /**
     * 3、动态编译业务层代码并打包，无条件，重新打包文件夹内所有文件
     * 对已生成的代码进行编译
     *
     * @param tableName 表名
     * @param generatorCodeVO 业务层交互数据传输类
     * @param res 结果输出字符串
     * @return void
     * @Author HuQi
     * @create 2021-08-27 17:29
     */
    @Transactional(rollbackFor = Exception.class)
    public void createAutoCompile(String tableName, GeneratorCodeDTO generatorCodeVO, StringBuilder res) {
        log.info(tableName + MESSAGE_INFO_GENERATOR_COMPILE);
        if (!generatorCodeService.mavenCompile("", SETTING_MAVEN_COMPILE_METHOD, generatorCodeVO, true)) {
            log.info(tableName + MESSAGE_ERROR_GENERATOR_COMPILE);
            ServiceUtils.throwException(res + MESSAGE_ERROR_GENERATOR_COMPILE);
        }else {
            log.info(tableName + MESSAGE_SUCCESS_GENERATOR_COMPILE);
            res.append(MESSAGE_SUCCESS_GENERATOR_COMPILE).append("    ");
        }
    }

    /**
     * 4、动态加载代码，加载编译完后的class字节码代码文件
     *
     * @param generatorTable 代码生成器表实体类
     * @param tableName 表名
     * @param generatorCodeVO 业务层交互数据传输类
     * @param res 结果输出字符串
     * @return void
     * @Author HuQi
     * @create 2021-08-27 17:30
     */
    @Transactional(rollbackFor = Exception.class)
    public void createAutoLoad(GeneratorCodeDO generatorTable, String tableName, GeneratorCodeDTO generatorCodeVO, StringBuilder res) {
        if (generatorTable == null || StringUtils.isEmpty(generatorTable.getIsGeneratorCode()) ||generatorTable.getIsGeneratorCode() == 0){
            ServiceUtils.throwException(tableName + MESSAGE_ERROR_GENERATOR_LOAD_START);
            return;
        }
        List<String> loadResult;
        try {
            loadResult = generatorCodeService.loadClass("", generatorCodeVO);
            if (!loadResult.isEmpty()){
                generatorTable.setIsGeneratorLoad(1);
                generatorCodeService.updateGeneratorTable(generatorTable);
            }
            log.info(tableName + MESSAGE_SUCCESS_GENERATOR_LOAD + loadResult);
            res.append(MESSAGE_SUCCESS_GENERATOR_LOAD).append(loadResult).append("    ");
        }catch (BeanDefinitionOverrideException ex){
            log.error(tableName + MESSAGE_ERROR_GENERATOR_LOAD_EXIST);
            ServiceUtils.throwException(res + MESSAGE_ERROR_GENERATOR_LOAD_EXIST);
        }catch (Exception e) {
            log.error(tableName + MESSAGE_ERROR_GENERATOR_LOAD);
            ServiceUtils.throwException(res + MESSAGE_ERROR_GENERATOR_LOAD + e.getMessage());
        }
    }

    /**
     * 移除代码生成器：用户通过输入表名移除有关的代码生成器，同时也可设定只移除部分内容
     *
     * @param tableName 数据库表名
     * @param isDeleteTable 是否移除相关表
     * @param isDeleteCode 是否移除相关代码
     * @param isDeleteBean 是否移除相关Bean
     * @return org.jeecg.common.api.vo.Result
     * @Author HuQi
     * @create 2021-08-02 17:18
     */
    @ApiOperation(value = "移除代码生成器", notes = "用户输入表的表名")
    @PostMapping(value = "/removeGenerator")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> removeGenerator(@ApiParam(value = "数据库表名", required = true) @RequestParam String tableName,
                                          @ApiParam(value = "是否移除相关表") @RequestParam(defaultValue = "1") Integer isDeleteTable,
                                          @ApiParam(value = "是否移除相关代码") @RequestParam(defaultValue = "1") Integer isDeleteCode,
                                          @ApiParam(value = "是否移除相关Bean") @RequestParam(defaultValue = "1") Integer isDeleteBean)
    {
        GeneratorCodeDTO generatorCodeVO = new GeneratorCodeDTO();
        setting = generatorCodeVO.getGeneratorSetting();
        String tableNamePlus = setting.getString(SETTING_TABLE_PREFIX) + tableName;
        GeneratorCodeDO generatorTable = generatorCodeService.getGeneratorTable(tableName);
        StringBuilder res = new StringBuilder(MESSAGE_INFO_GENERATOR_START);
        generatorCodeVO.setUserName(getUser().getUsername());

        if (!generatorTable.getCreateBy().equals(generatorCodeVO.getUserName())){
            return Result.Error(MESSAGE_ERROR_TABLE_ALREADY_USE);
        }

        if (isDeleteBean == 1) {
            try {
                if (generatorCodeService.removeBean(oConvertUtils.camelName(tableName))) {
                    log.info(MESSAGE_SUCCESS_REMOVE_GENERATOR_BEAN);
                    generatorTable.setIsGeneratorLoad(0);
                    // 防止二次注入时未开启接口导致接口后续无法开启
                    generatorTable.setIsEnableUrl(1);
                    generatorCodeService.updateGeneratorTable(generatorTable);
                    res.append(MESSAGE_SUCCESS_REMOVE_GENERATOR_BEAN).append("    ");
                } else {
                    return Result.Error(res + MESSAGE_ERROR_REMOVE_GENERATOR_BEAN);
                }
            }catch (Exception e){
                return Result.Error(res + e.getMessage() + MESSAGE_ERROR_REMOVE_GENERATOR_BEAN);
            }
        }

        if (isDeleteCode == 1) {
            String removePath = generatorCodeVO.getProjectPath() + setting.getString(SETTING_AUTO_GENERATOR_PATH)
                    + "/" + SETTING_USER_PATH + "/" + getUser().getUsername() + "/src/main/java" + File.separator
                    + setting.getString(SETTING_PARENT).replace(".","/") + File.separator
                    + oConvertUtils.camelNameCapFirst(tableName);
            log.info(MESSAGE_INFO_REMOVE_GENERATOR_CODE + removePath);
            deleteCode(generatorTable, tableName, generatorCodeVO, res, removePath);
        }

        if (isDeleteTable == 1) {
            if (generatorCodeService.removeTable(generatorTable, tableNamePlus)){
                log.info(MESSAGE_SUCCESS_REMOVE_GENERATOR_TABLE + tableNamePlus);
                res.append(MESSAGE_SUCCESS_REMOVE_GENERATOR_TABLE).append(tableNamePlus).append("    ");
            }else {
                return Result.Error(res + MESSAGE_ERROR_REMOVE_GENERATOR_TABLE);
            }
        }

        return Result.OK(res + MESSAGE_SUCCESS_REMOVE_GENERATOR);
    }

    /**
     * 删除代码生成器生成的代码
     *
     * @param generatorTable 代码生成器表实体类
     * @param tableName 表名
     * @param generatorCodeVO 业务层交互数据传输类
     * @param res 结果输出字符串
     * @param removePath 代码移除路径
     * @return void
     * @Author HuQi
     * @create 2021-08-28 14:55
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCode(GeneratorCodeDO generatorTable, String tableName, GeneratorCodeDTO generatorCodeVO, StringBuilder res, String removePath) {
        try {
            FileUtil.deleteDir(new File(removePath));
            new Thread(() -> generatorCodeService.mavenCompileLock("", "compile", generatorCodeVO, false)).start();
            generatorTable.setIsGeneratorCode(0);
            generatorTable.setCodePath("");
            generatorCodeService.updateGeneratorTable(generatorTable);
            if (TRUE_STR.equals(setting.getString(SETTING_IS_PERSISTENT)) &&
                    SETTING_PERSISTENT_METHOD_GITLAB.equals(setting.getString(SETTING_PERSISTENT_METHOD))) {
                // 开启线程删除目录
                String path = setting.getString(SETTING_GITLAB_TREE) + "/" + oConvertUtils.camelNameCapFirst(tableName);
                new Thread(() -> generatorCodeService.deletePathFromGitLab(path, setting)).start();
            }
            log.info(MESSAGE_ERROR_REMOVE_GENERATOR_CODE + removePath);
            res.append(MESSAGE_ERROR_REMOVE_GENERATOR_CODE).append(removePath).append("    ");
        } catch (IOException e) {
            log.error(e.getMessage());
            ServiceUtils.throwException(res + MESSAGE_ERROR_CLEAN_DIR);
        }
    }

    /**
     * 清空代码生成器此功能只有admin账号管理员才有权限操作
     *
     * @return org.jeecg.common.api.vo.Result
     * @Author HuQi
     * @create 2021-08-02 17:23
     */
    @ApiOperation(value = "清空代码生成器", notes = "管理员admin账号操作。")
    @GetMapping("/cleanGenerator")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> cleanGenerator(){

        GeneratorCodeDTO generatorCodeVO = new GeneratorCodeDTO();
        if (generatorCodeService.cleanAllGenerator(generatorCodeVO)){
            return Result.OK(MESSAGE_SUCCESS_REMOVE_ALL_GENERATOR);
        }else {
            return Result.Error(MESSAGE_ERROR_REMOVE_ALL_GENERATOR);
        }

    }

    /**
     * 通过传入模块名以及对应的禁用、启用操作名，进行对相应的接口的操作
     *
     * @param moduleName 模块名
     * @param action 操作名
     * @return org.jeecg.common.api.vo.Result
     * @Author HuQi
     * @create 2021-08-12 09:39
     */
    @ApiOperation(value = "代码生成器的接口启用/禁用")
    @GetMapping("/optGeneratorURL")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> optGeneratorUrl(@ApiParam(value = "模块名 接口倒数第二级的地址", required = true)
                                          @RequestParam String moduleName,
                                          @ApiParam(value = "操作 启用：" + ENABLE_STR + "  禁用：" + DISABLE_STR, required = true)
                                          @RequestParam String action){
        GeneratorCodeDO generatorTable = generatorCodeService.getGeneratorTable(oConvertUtils.camelToUnderline(moduleName));
        if (ENABLE_STR.equals(action)){
            if (generatorCodeService.optGeneratorUrl(moduleName + C_CONTROLLER, action, generatorTable)) {
                return Result.OK(moduleName + MESSAGE_SUCCESS_OPT_URL_OPEN);
            }else {
                return Result.Error(moduleName + MESSAGE_ERROR_OPT_URL_OPEN);
            }
        }else if (DISABLE_STR.equals(action)){
            if (generatorCodeService.optGeneratorUrl(moduleName + C_CONTROLLER, action, generatorTable)) {
                return Result.OK(moduleName + MESSAGE_SUCCESS_OPT_URL_CLOSE);
            }else {
                return Result.Error(moduleName + MESSAGE_ERROR_OPT_URL_CLOSE);
            }
        }else {
            return Result.Error(MESSAGE_ERROR_OPT_URL_TYPE);
        }

    }
}
