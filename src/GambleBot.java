import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;

/**
 * 09.04.2018 | created by Lukas S
 */

public class GambleBot extends TelegramLongPollingBot {
    private final Integer ADMIN_ID = <MY_ID>;

    private int maxCrash;
    private ArrayList<KeyboardRow> keyboard;
    private KeyboardRow row;
    private KeyboardRow row2;
    private KeyboardRow row3;
    private SendMessage message;

    private ArrayList<Gambler> gamblers;

    public GambleBot() {
        super();
        maxCrash = 40;
        gamblers = new ArrayList<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update.getMessage().getText() + " | " + update.getMessage().getFrom().toString());
        Gambler currentUser = new Gambler(update.getMessage().getFrom());

        //Check if user is registered
        boolean found = false;
        for (Gambler gambler : gamblers) {
            if (gambler.equals(currentUser)) {
                found = true;
                currentUser = gambler;
                break;
            }
        }

        //If user was not registered yet, he will we registered and a message will be sent to the admin
        if (!found) {
            gamblers.add(currentUser);
            System.err.println(currentUser.user.getId() + " added!");

            if (currentUser.user.getId().equals(ADMIN_ID)) {
                SendMessage adminMsg = new SendMessage()
                        .setChatId("" + ADMIN_ID)
                        .setText(EmojiParser.parseToUnicode(":exclamation:") + currentUser.user.getId() + EmojiParser.parseToUnicode(":exclamation:") + " added!");

                try {
                    execute(adminMsg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }

        //Default message
        message = new SendMessage().setChatId(update.getMessage().getChatId())
                .setText("Ich bin der Gamble Crash Bot. Hallo!");

        //Text handling
        if (update.hasMessage() && update.getMessage().hasText()) {

            //Command handling
            if (update.getMessage().isCommand()) {
                switch (update.getMessage().getText().trim()) {
                    default:
                        message.setText("Unbekannter Befehl!");
                        break;
                    case "/pot":
                        message.setText("Im Pot sind " + currentUser.pot + " Coins");
                        break;
                    case "/roll":
                        if (currentUser.pot == 0) {
                            message.setText("Pot ist leer!");
                            break;
                        }

                        int r = 1;

                        for (int i = 1; i <= maxCrash; i++) {
                            if (i == 1 && Math.random() > 0.5) {
                                r = i;
                            } else if (i > 1 && Math.random() > 0.20) {
                                r = i;
                            } else {
                                r = i;
                                break;
                            }
                        }

                        currentUser.history += "x" + r;

                        if (r >= currentUser.cashout) {
                            currentUser.coins += currentUser.pot * currentUser.cashout;
                            message.setText("Du bist bei x" + currentUser.cashout + " rausgegangen (+" + (currentUser.pot * currentUser.cashout) + "), bei x" + r + " war der Crash! " + EmojiParser.parseToUnicode(":+1:") + "\nDu hast jetzt " + currentUser.coins + " Coins");
                            currentUser.history += EmojiParser.parseToUnicode(":+1:|");
                            currentUser.pot = 0;
                        } else {
                            message.setText("Du bist bei x" + r + " gecrasht! Bei x" + currentUser.cashout + " wäre der Cashout gewesen! " + EmojiParser.parseToUnicode(":-1:") + "\nDu hast noch " + currentUser.coins + " Coins");
                            currentUser.history += EmojiParser.parseToUnicode(":-1:|");
                            currentUser.pot = 0;
                        }

                        break;
                    case "/clear":
                        currentUser.coins += currentUser.pot;
                        currentUser.pot = 0;
                        message.setText("Pot ist jetzt bei " + currentUser.pot + " Coins");
                        break;
                    case "/coins":
                        message.setText("Du hast " + currentUser.coins + " Coins");
                        break;
                    case "/cashout":
                        message.setText("Dein Cashout ist bei x" + currentUser.cashout);
                        break;
                    case "/history":
                        message.setText(currentUser.history);
                        break;
                    case "/admin":
                        if (currentUser.user.getId().equals(ADMIN_ID)) {
                            StringBuilder s = new StringBuilder();
                            for (Gambler gambler : gamblers) {
                                s.append(gambler.user.getId()).append(" | ").append(gambler.user.getFirstName()).append(" | Coins: ").append(gambler.coins);
                                s.append("\n");
                            }

                            SendMessage adminMsg = new SendMessage()
                                    .setChatId("" + ADMIN_ID)
                                    .setText(EmojiParser.parseToUnicode(":exclamation:") + s.toString());

                            try {
                                execute(adminMsg);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        } else {
                            message.setText("Nur für Admins!");
                        }

                        break;
                }

                //Coin add handling
            } else if (update.getMessage().getText().trim().startsWith("+")) {
                //Coins werden zum pot hinzugefügt

                try {
                    int v = Integer.valueOf(update.getMessage().getText().trim());
                    if (v > currentUser.coins) {
                        message.setText("So viele Coins hast du nicht!");
                    } else {
                        currentUser.coins -= v;
                        currentUser.pot += v;
                        message.setText("Pot ist jetzt: " + currentUser.pot);
                    }
                } catch (NumberFormatException n) {
                    message.setText("Keine Zahl!");
                }

                //Cashout set handler
            } else if (update.getMessage().getText().trim().startsWith("x")) {
                try {
                    int v = Integer.valueOf(update.getMessage().getText().replace("x", " ").trim());
                    if (v < 2) {
                        message.setText("Cashout zu klein! Wähle zwischen x0 und x" + maxCrash + "!");
                    } else if (v > maxCrash) {
                        message.setText("Cashout zu hoch! Das Maximum ist x" + maxCrash + "!");
                    } else {
                        currentUser.cashout = v;
                        message.setText("Cashout ist jetzt bei x" + currentUser.cashout);
                    }
                } catch (NumberFormatException n) {
                    message.setText("Keine Zahl!");
                }
            }


            keyboard = new ArrayList<>();
            row = new KeyboardRow();
            row2 = new KeyboardRow();
            row3 = new KeyboardRow();

            row.add("+1");
            row.add("+5");
            row.add("x2");
            row.add("x3");
            row2.add("/clear");
            row2.add("/roll");
            row3.add("+10");
            row3.add("+" + currentUser.coins);
            row3.add("x4");
            row3.add("x5");

            keyboard.add(row);
            keyboard.add(row2);
            keyboard.add(row3);
            message.setReplyMarkup(new ReplyKeyboardMarkup().setKeyboard(keyboard));

            try {
                System.out.println("-> " + message.getText() + "\n---");
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "crash_bot";
    }

    @Override
    public String getBotToken() {
        return <MY_BOT_TOKEN>;
    }

    private class Gambler {
        User user;

        int coins;
        int pot;
        int cashout;
        String history;

        public Gambler(User user) {
            this.user = user;
            coins = 100;
            pot = 0;
            cashout = 2;
            history = "|";
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == this.getClass() && ((Gambler) obj).user.getId().equals(this.user.getId());
        }
    }

}
