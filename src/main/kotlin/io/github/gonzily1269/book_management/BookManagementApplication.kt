package io.github.gonzily1269.book_management

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BookManagementApplication

fun main(args: Array<String>) {
	runApplication<BookManagementApplication>(*args)
}
