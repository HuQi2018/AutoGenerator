package org.jeecg.util;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jeecg.entity.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * What purpose
 *
 * @author XuDeQing
 * @date 2021-03-2021/3/11 15:25
 */
public class ServiceUtils {
    private ServiceUtils() {
    }

    public static void throwIfFailed(boolean success, String message) {
        throwIfFailed(success, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static void throwIfFailed(boolean success, HttpStatus status, String message) {
        if (!success) throw new HttpServerErrorException(status, message);
    }

    public static void throwIfFailed(BooleanSupplier supplier, HttpStatus status, String message) {
        if (!supplier.getAsBoolean()) throw new HttpServerErrorException(status, message);
    }

    public static void throwIfFailed(BooleanSupplier supplier, String message) {
        throwIfFailed(supplier, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static <T> void throwIfNull(Supplier<T> supplier, HttpStatus status, String message) {
        if (Objects.isNull(supplier.get())) throw new HttpServerErrorException(status, message);
    }

    public static <T> void throwIfNull(Supplier<T> supplier, String message) {
        throwIfNull(supplier, HttpStatus.INTERNAL_SERVER_ERROR,message);
    }

    public static void throwException(String message){
        throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,message);
    }
}
