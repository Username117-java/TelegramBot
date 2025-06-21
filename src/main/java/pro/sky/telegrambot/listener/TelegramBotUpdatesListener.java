package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    // Паттерн для распознавания сообщений с напоминаниями
    private static final Pattern TASK_PATTERN = Pattern.compile(
            "(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})\\s+([\\W\\w]+)"
    );

    // Форматтер для преобразования строки с датой в LocalDateTime
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message() != null && update.message().text() != null) {
                Long chatId = update.message().chat().id();
                String messageText = update.message().text();

                if (messageText.equals("/start")) {
                    String greetingText = "Привет! Я твоя напоминалка. " +
                            "Отправь мне сообщение в формате: дд.мм.гггг чч:мм Текст напоминания " +
                            "и жди сообщения от меня в назначенное время";
                    sendMessage(chatId, greetingText);
                } else {

                    // Попытка распознать сообщение как напоминание
                    Matcher matcher = TASK_PATTERN.matcher(messageText);
                    if (matcher.matches()) {
                        try {
                            // Парсим дату и время из сообщения
                            LocalDateTime notificationTime = LocalDateTime.parse(matcher.group(1), DATE_TIME_FORMATTER);
                            String notificationText = matcher.group(2);

                            // Создаем и сохраняем задачу
                            NotificationTask task = new NotificationTask(chatId, notificationText, notificationTime);
                            notificationTaskRepository.save(task);

                            sendMessage(chatId, "Напоминание успешно запланировано!");
                        } catch (DateTimeParseException e) {
                            sendMessage(chatId, "Некорректный формат даты и времени. Используйте формат: дд.мм.гггг чч:мм Текст напоминания");
                        }
                    } else {
                        sendMessage(chatId, "Некорректный формат сообщения. Используйте формат: дд.мм.гггг чч:мм Текст напоминания");
                    }
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        telegramBot.execute(message);
    }
}


