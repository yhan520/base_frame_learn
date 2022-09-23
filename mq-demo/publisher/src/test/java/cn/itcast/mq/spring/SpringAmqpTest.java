package cn.itcast.mq.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void simpleQueueTest(){
        //队列名称
        String queueName = "simple.queue";
        //消息内容
        String message = "hello, spring amqp!";
        //把消息发送到队列
        rabbitTemplate.convertAndSend(queueName, message);
    }

    @Test
    public void testWorkQueue() throws InterruptedException {
        String queueName = "simple.queue";
        String message = "hello, message_";

        for (int i = 0; i < 100; i++) {
            rabbitTemplate.convertAndSend(queueName, message + i);
            Thread.sleep(20);
        }
    }

    @Test
    public void testFanoutQueue(){
        // 队列名称
        String exchangeName = "fanout.exchange";
        // 消息
        String message = "hello, everyone!";

        rabbitTemplate.convertAndSend(exchangeName, "", message);
    }

    @Test
    public void testDirectExchange() {
        // 队列名称
        String exchangeName = "direct.exchange";
        // 消息
        String message = "hello, green!";
        rabbitTemplate.convertAndSend(exchangeName, "green", message);
    }

    @Test
    public void testTopicExchange(){
        //队列名称
        String exchangeName = "topic.exchange";
        // 消息
        //String message = "good weather";
        Map<String, String> message = new HashMap<>();
        message.put("name", "杨涵");
        message.put("age", "24");
        rabbitTemplate.convertAndSend(exchangeName, "china.#", message);
    }
}
