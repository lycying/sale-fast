package com.github.xjjdog.seckill.core.components.queue;

import com.github.xjjdog.seckill.core.Factory;
import com.github.xjjdog.seckill.core.Holder;
import com.github.xjjdog.seckill.core.components.queue.QueueProcessorKafka;
import com.github.xjjdog.seckill.core.components.stock.StockService;
import com.github.xjjdog.seckill.core.entity.ActionSell;
import com.github.xjjdog.seckill.core.target.Target;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
/**
 * kafka test case not running for junit .
 */
public class QueueProcessorKafkaTest {
    public static void main(String[] args) throws Exception {
        test();
    }

    static StockService stockService;
    static QueueProcessorKafka queueProcessor;

    public static void test() throws Exception {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "localhost");
        properties.put(ProducerConfig.ACKS_CONFIG, "1");
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, "16384");
        properties.put(ProducerConfig.RETRIES_CONFIG, "0");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());


        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test-seckill-consumer");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());


        stockService = Holder.getInstance().getStockService();
        queueProcessor = new QueueProcessorKafka();

        queueProcessor.configure(properties);
        queueProcessor.setTopic("test-seckill");
        queueProcessor.start();

        Target target = Holder.getInstance().getTargetService().getTarget("1");
        int initStock = Factory.InitStock;
        ActionSell actionSell = new ActionSell();
        actionSell.setCount(10);

        stockService.fillStock(target);
        boolean result = queueProcessor.producer(target, actionSell);
        assertEquals(result, true);


        //wait to consumer
        Thread.sleep(1000 * 5);
        int stockNumber = stockService.stockNumber(target);
        assertEquals(Factory.InitStock - 10, stockNumber);

        stockService.cleanup(target);


        actionSell.setCount(101);
        stockService.fillStock(target);
        result = queueProcessor.producer(target, actionSell);
        assertEquals(result, false);

        System.exit(-1);
    }
}
