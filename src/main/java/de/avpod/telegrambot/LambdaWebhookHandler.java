//package de.avpod.telegrambot;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.LambdaLogger;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import org.telegram.telegrambots.api.methods.send.SendMessage;
//import org.telegram.telegrambots.api.objects.Message;
//import org.telegram.telegrambots.api.objects.Update;
//
//import java.util.Arrays;
//import java.util.Base64;
//
//public class LambdaWebhookHandler implements RequestHandler<Update, SendMessage> {
//
//    public SendMessage handleRequest(Update request, Context context) {
//        LambdaLogger logger = context.getLogger();
//
//        if(!request.hasMessage())
//            return null;
//
//        logger.log(String.format(
//                "Processing update from: %s" + request.getMessage().getFrom().getUserName()
//        ));
//        Message message = request.getMessage();
//        return new SendMessage(message.getChatId(), "Here si echo for you:" +message.getText());
//    }
//}
