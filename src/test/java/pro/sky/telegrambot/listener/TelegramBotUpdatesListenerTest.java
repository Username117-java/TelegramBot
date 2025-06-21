package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramBotUpdatesListenerTest {

    @Mock
    private TelegramBot telegramBot;

    @Mock
    private NotificationTaskRepository notificationTaskRepository;

    @InjectMocks
    private TelegramBotUpdatesListener listener;

    private Update createUpdate(Long chatId, String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.text()).thenReturn(text);

        return update;
    }

    @Test
    void processStartCommandTest() {
        Update update = createUpdate(123L, "/start");
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);

        listener.process(Collections.singletonList(update));

        verify(telegramBot).execute(messageCaptor.capture());
        SendMessage actual = messageCaptor.getValue();
        assertEquals(123L, actual.getParameters().get("chat_id"));
        assertTrue(((String) actual.getParameters().get("text")).contains("Привет! Я твоя напоминалка."));
    }

    @Test
    void processValidTaskTest() {
        Update update = createUpdate(123L, "01.01.2025 12:00 Тест напоминания");
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        ArgumentCaptor<NotificationTask> taskCaptor = ArgumentCaptor.forClass(NotificationTask.class);

        listener.process(Collections.singletonList(update));

        verify(notificationTaskRepository).save(taskCaptor.capture());
        verify(telegramBot).execute(messageCaptor.capture());

        NotificationTask savedTask = taskCaptor.getValue();
        assertEquals(123L, savedTask.getChatId());
        assertEquals("Тест напоминания", savedTask.getMessage());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), savedTask.getNotificationTime());

        SendMessage response = messageCaptor.getValue();
        assertEquals("Напоминание успешно запланировано!", response.getParameters().get("text"));
    }

    @Test
    void processInvalidDateFormatTest() {
        Update update = createUpdate(123L, "01-01-2025 12:00 Тест напоминания");
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);

        listener.process(Collections.singletonList(update));

        verify(telegramBot).execute(messageCaptor.capture());
        SendMessage response = messageCaptor.getValue();
        assertEquals("Некорректный формат сообщения. Используйте формат: дд.мм.гггг чч:мм Текст напоминания",
                response.getParameters().get("text"));
    }

    @Test
    void processInvalidMessageFormatTest() {
        Update update = createUpdate(123L, "Просто текст без даты");
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);

        listener.process(Collections.singletonList(update));

        verify(telegramBot).execute(messageCaptor.capture());
        SendMessage response = messageCaptor.getValue();
        assertEquals("Некорректный формат сообщения. Используйте формат: дд.мм.гггг чч:мм Текст напоминания",
                response.getParameters().get("text"));
    }


}
