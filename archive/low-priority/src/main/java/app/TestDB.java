package app;

import util.DB;

public class TestDB {
    public static void main(String[] args) {
        try {
            System.out.println(DB.getConnection());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
