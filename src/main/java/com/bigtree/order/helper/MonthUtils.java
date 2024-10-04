package com.bigtree.order.helper;


import java.time.Month;

public class MonthUtils {

    public static String[] getMonths(){
        return new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    }

    public static String getShortName(Month month) {
        switch (month) {
            case JANUARY -> {
                return "Jan";
            }
            case FEBRUARY -> {
                return "Feb";
            }
            case MARCH -> {
                return "Mar";
            }
            case APRIL -> {
                return "Apr";
            }
            case MAY -> {
                return "May";
            }
            case JUNE -> {
                return "Jun";
            }
            case JULY -> {
                return "Jul";
            }
            case AUGUST -> {
                return "Aug";
            }
            case SEPTEMBER -> {
                return "Sep";
            }
            case OCTOBER -> {
                return "Oct";
            }
            case NOVEMBER -> {
                return "Nov";
            }
            case DECEMBER -> {
                return "Dec";
            }
            default -> {
                return "";
            }
        }
    }
}
