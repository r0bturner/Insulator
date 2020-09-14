package insulator.lib.kafka

import insulator.lib.configuration.model.Cluster
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ConsumerTest : FunSpec({

    test("start happy path") {
        // arrange
        val messages = mutableListOf<String>()
        val sut = Consumer(Cluster.empty())
        // act
        sut.start("testTopic", ConsumeFrom.Beginning, DeserializationFormat.String) { messages.addAll(it.map { record -> record.b }) }
        // assert
        delay(200)
        sut.stop()
        messages.size shouldBe 1
    }

    test("start happy path - now") {
        // arrange
        val messages = mutableListOf<String>()
        val sut = Consumer(Cluster.empty())
        // act
        sut.start("testTopic", ConsumeFrom.Now, DeserializationFormat.String) { messages.addAll(it.map { record -> record.b }) }
        // assert
        delay(200)
        sut.stop()
        messages.size shouldBe 0
    }

    test("isRunning") {
        // arrange
        val messages = mutableListOf<String>()
        val sut = Consumer(Cluster.empty())
        // act
        sut.start("testTopic", ConsumeFrom.Now, DeserializationFormat.String) { messages.addAll(it.map { record -> record.b }) }
        // assert
        sut.isRunning() shouldBe true
        sut.stop()
        sut.isRunning() shouldBe false
    }

    test("stop if not running") {
        // arrange
        val sut = Consumer(Cluster.empty())
        // act/assert
        sut.stop()
    }

    lateinit var fixture: TestConsumerFixture
    beforeTest {
        fixture = TestConsumerFixture()
        startKoin { modules(fixture.koinModule) }
    }

    afterTest {
        stopKoin()
        fixture.mockConsumer.close()
    }
})

class TestConsumerFixture {

    val mockConsumer = MockConsumer<Any, Any>(OffsetResetStrategy.EARLIEST).also {
        val topicName = "testTopic"
        it.updatePartitions(topicName, listOf(PartitionInfo(topicName, 0, null, null, null)))
        it.updateBeginningOffsets(mapOf(TopicPartition(topicName, 0) to 0L))
        it.updateEndOffsets(mapOf(TopicPartition(topicName, 0) to 1L))
        it.assign(listOf(TopicPartition(topicName, 0)))
        it.addRecord(ConsumerRecord(topicName, 0, 0L, "key", "value"))
    }

    val koinModule = module {
        scope<Cluster> {
            // Consumers
            scoped<org.apache.kafka.clients.consumer.Consumer<Any, Any>>() {
                mockConsumer
            }
        }
    }
}
