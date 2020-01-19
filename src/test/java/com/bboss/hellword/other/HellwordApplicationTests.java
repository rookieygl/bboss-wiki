package com.bboss.hellword.other;

import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(SpringRunner.class)
//@SpringBootTest
public class HellwordApplicationTests {

    @Test
    public void testRegex() {
        String s = "2019121710.ctr";
        boolean matches = Pattern.matches("20[0-9]*", s);
        if (matches) {
            System.out.println("test");
        }else {
            System.out.println("false");
        }

    }


    @Test
    public void testInfolog(){

    }
}
