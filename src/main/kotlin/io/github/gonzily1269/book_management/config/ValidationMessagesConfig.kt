package io.github.gonzily1269.book_management.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer

/**
 * バリデーションメッセージ設定クラス
 *
 * application.propertiesからバリデーションメッセージを読み込むための設定を提供する。
 * PropertySourcesPlaceholderConfigurerをBean登録することで、
 * @Valueアノテーションでプロパティを注入可能にする。
 */
@Configuration
class ValidationMessagesConfig {

    /**
     * application.propertiesからvalidationメッセージを読み込むためのBean
     *
     * これにより、@Valueアノテーションでプロパティを注入できる。
     *
     * @return PropertySourcesPlaceholderConfigurer
     */
    @Bean
    fun propertyConfigurer(): PropertySourcesPlaceholderConfigurer {
        return PropertySourcesPlaceholderConfigurer()
    }
}
