package com.springmart.exception;

/**
 * 商品やユーザーなどのリソースが見つからない場合に使用する例外クラスです。
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
