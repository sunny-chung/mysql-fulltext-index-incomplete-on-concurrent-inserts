import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.UUID
import kotlin.test.assertEquals

class MysqlConnector {
    var connection: Connection = DriverManager.getConnection("jdbc:mysql://localhost:7000/?user=root&password=123")

    init {
        connection.autoCommit = true
    }

    fun executeSQL(sql: String) {
        val statement = connection.createStatement()
        try {
            println("SQL: $sql")
            statement.execute(sql)
        } finally {
            statement.close()
        }
    }

    fun executeQueryCount(sql: String): Int {
        val statement = connection.createStatement()
        try {
            println("SQL: $sql")
            val resultSet = statement.executeQuery(sql)
            resultSet.next()
            return resultSet.getInt(1)
        } finally {
            statement.close()
        }
    }
}

class FulltextIndexTest {
    companion object {
        fun uuid() = UUID.randomUUID().toString()

        @JvmStatic
        @BeforeAll
        fun init() {
            val initializer = MysqlConnector()
            initializer.executeSQL("CREATE DATABASE IF NOT EXISTS d")
            initializer.executeSQL("""
                CREATE TABLE IF NOT EXISTS `d`.`t` (
                  `id` varchar(255) NOT NULL,
                  `channel_id` varchar(255) NOT NULL,
                  `content` text NOT NULL,
                  PRIMARY KEY (`id`),
                  FULLTEXT KEY `idx_my_fulltext_index` (`content`) WITH PARSER `ngram`
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
            """.trimIndent())

            // create some noise data beforehand to avoid hitting the "50% rule"
            val nonTestWords = "wait walk wall want war watch water way we weapon wear week weight well west western what whatever when where whether which while white who whole whom whose why wide wife will win wind window wish with within without woman wonder word work worker world worry would write writer".split(" ")
            repeat(100) {
                initializer.executeSQL("INSERT INTO `d`.`t` (`id`, `channel_id`, `content`) VALUES ('${uuid()}', '${uuid()}', '${nonTestWords.shuffled().take(20).joinToString(" ")}')")
            }
        }
    }

    val WORDS = "after again against age agency agent ago agree agreement ahead air all allow almost alone along already also although always American among amount analysis and animal another answer any anyone anything".split(" ")

    @Test
    fun realTestCase() {
        val records = mutableListOf<String>()
        repeat(15) { records += WORDS.subList(0, 10).shuffled().joinToString(separator = " ") }
        repeat(10) { records += WORDS.subList(3, 12).joinToString(separator = " ") }
        repeat(25) { records += WORDS.subList(7, 18).shuffled().joinToString(separator = " ") }
        records.shuffle()

        val channelId = uuid()

        val threads = mutableListOf<Thread>()
        records.forEach { content ->
            threads += Thread {
                val mysql = MysqlConnector()
                mysql.executeSQL(
                    "INSERT INTO `d`.`t` (`id`, `channel_id`, `content`) VALUES ('${uuid()}', '$channelId', '$content')"
                )
            }
        }
        threads.forEach { thread -> thread.start() }
        threads.forEach { thread -> thread.join() }

        val mysql = MysqlConnector()
        val matchArgument = WORDS.subList(7, 10).joinToString(separator = " ") { word -> "+$word" }
        val resultCount = mysql.executeQueryCount("SELECT COUNT(*) AS count FROM `d`.`t` WHERE MATCH(t.content) AGAINST ('$matchArgument' IN BOOLEAN MODE) AND channel_id = '$channelId'")
        println("Query result: $resultCount")

        assertEquals(15 + 10 + 25, resultCount)
    }
}
