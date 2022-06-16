package org.jeecg.util;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.entity.LoginUser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * @Description:
 * @author: HuQi
 * @date: 2021年07月31日 17:45
 */
@Slf4j
public class GeneratorCodeUtil {

    /**
     * @Description: String字符串转Base64编码
     * @param content 字符串内容
     * @return java.lang.String
     * @Author HuQi
     * @create 2021-08-02 22:01
     */
    public static String stringToBase64(String content){
        final Base64.Encoder encoder = Base64.getEncoder();
        byte[] textByte = content.getBytes(StandardCharsets.UTF_8);
        content = encoder.encodeToString(textByte);
        return content;
    }

    /**
     * @Description: Base64编码转String字符串
     * @param content 字符串内容
     * @return java.lang.String
     * @Author HuQi
     * @create 2021-08-02 22:00
     */
    public static String base64ToString(String content){
        if (content==null){
            return null;
        }
        final Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(content), StandardCharsets.UTF_8);
    }

    /**
     * @Description: 复制Resources目录下的文件至指定目录下
     * @param fromResourcesPath Resources目录下的文件路径
     * @param toPath 目标文件路径
     * @Author HuQi
     * @create 2021-08-02 21:59
     */
    public static void copyTemplatesFile(String fromResourcesPath, String toPath){
        try {
            Files.copy(GeneratorCodeUtil.class.getResourceAsStream(fromResourcesPath), new File(toPath).toPath());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("复制模板文件失败：" + fromResourcesPath + "  TO   " + toPath);
        }
    }

    /**
     * @Description: 获取用户登录信息
     * @return org.jeecg.common.system.vo.LoginUser
     * @Author HuQi
     * @create 2021-08-02 21:58
     */
    public static LoginUser getUser(){
        LoginUser loginUser = new LoginUser();
        loginUser.setUsername("admin");
        return loginUser;
    }

    /**
     * 将json转化为map
     * @param json 最起初的json转map
     * @return Map
     */
    public static Map<String, Object> jsonToMap(String json) {
        Map<String, Object> rs = new HashMap<>(1);
        try {
            rs = (Map) JSON.parse(json);
        }catch (Exception e){
            ServiceUtils.throwException("请检查JSON字符串是否符合规范！" + e.getMessage());
        }
        return rs;
    }

    /**
     * 将map的key转化为string
     * @param map map
     */
    public static String mapKeyToString(String pre, Map map) {
        StringBuilder key= new StringBuilder();
        for (Object m : map.entrySet()) {
            key.append(pre).append(((Map.Entry) m).getKey()).append(",");
        }
        return key.toString();
    }

    /**
     * 将map的key转化为list
     * @param map map
     */
    public static List<String> mapKeyToList(Map<String, Object> map){
        List<String> list = new ArrayList<>();
        for (Object m : map.entrySet()) {
            list.add((String) ((Map.Entry) m).getKey());
        }
        return list;
    }

    /**
     * 将map的value转化为list
     * @param map map
     */
    public static List<Object> mapValueToList(Map<String, Object> map){
        List<Object> list = new ArrayList<>();
        for (Object m : map.entrySet()) {
            list.add(((Map.Entry) m).getValue());
        }
        return list;
    }
}
