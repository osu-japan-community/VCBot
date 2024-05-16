package net.mamesosu;

import net.mamesosu.Object.Bot;

public class Main {

    public static Bot bot;

    public static void main(String[] args) {
        bot = new Bot();
        bot.loadJDA();
    }
}