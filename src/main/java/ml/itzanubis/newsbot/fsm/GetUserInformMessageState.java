package ml.itzanubis.newsbot.fsm;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import ml.itzanubis.newsbot.TelegramBot;
import ml.itzanubis.newsbot.entity.Channel;
import ml.itzanubis.newsbot.service.ChannelService;
import ml.itzanubis.newsbot.telegram.machine.FieldStateMachine;
import ml.itzanubis.newsbot.telegram.machine.UserState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class GetUserInformMessageState implements UserState {
    private final TelegramBot bot;

    private final ChannelService channelService;

    @Autowired
    public GetUserInformMessageState(TelegramBot bot, ChannelService channelService) {
        this.bot = bot;
        this.channelService = channelService;
    }

    @Override
    @SneakyThrows
    public void state(@NonNull User user, @NonNull Message message, @NonNull Object[] callbackData) {
        val replyKeyboardMarkup = new InlineKeyboardMarkup();
        val applyButton = new InlineKeyboardButton();
        val declineButton = new InlineKeyboardButton();
        val buttonsRow = new ArrayList<List<InlineKeyboardButton>>();
        val channel = (Channel) callbackData[0];

        applyButton.setCallbackData("accept-news");
        declineButton.setCallbackData("decline-news");

        buttonsRow.add(List.of(applyButton, declineButton));

        applyButton.setText("Подтвердить");
        declineButton.setText("Отклонить");

        replyKeyboardMarkup.setKeyboard(buttonsRow);

        if (message.hasPhoto()) {
            val inform = message.getCaption();
            val informedMessage = new SendPhoto();
            val photo = bot.downloadFile(bot.execute(new GetFile(message.getPhoto().get(2).getFileId())));

            informedMessage.setCaption("Вам пришла новость: " + inform);
            informedMessage.setPhoto(new InputFile(photo));
            informedMessage.setChatId(channel.getUserId());
            informedMessage.setReplyMarkup(replyKeyboardMarkup);

            bot.execute(informedMessage);

            FieldStateMachine.cancelState(user);
            return;
        }

        val inform = message.getText();
        val informedMessage = new SendMessage();

        informedMessage.setText("Вам пришла новость: " + inform);
        informedMessage.setChatId(channel.getUserId());
        informedMessage.setReplyMarkup(replyKeyboardMarkup);

        bot.execute(informedMessage);

        FieldStateMachine.clearCallback(this);
        FieldStateMachine.cancelState(user);
    }
}