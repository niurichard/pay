package com.lhs.pay.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * TestBase
 *
 * @author longhuashen
 * @since 16/4/16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations= "classpath:spring/spring-context.xml")
public class TestBase {

    @Test
    public void testInsert() {

    }
}
