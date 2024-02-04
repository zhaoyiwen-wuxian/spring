package com.trench;


import com.trench.impl.AppConfig;
import com.trench.springUtil.TrenchApplicationContest;

public class Application {

    public static void main(String[] args) {
        TrenchApplicationContest contest=new TrenchApplicationContest(AppConfig.class);
         System.out.println(contest.getBean("userServer"));

    }

}
