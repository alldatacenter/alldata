package com.datasophon.worker.actor;


import akka.actor.SupervisorStrategy;
import akka.japi.Function;

import java.sql.SQLException;

public class SupervisorFunction implements Function<Throwable, SupervisorStrategy.Directive> {
    @Override
    public SupervisorStrategy.Directive apply(Throwable param) throws Exception {
        if(param instanceof ArithmeticException){
            System.out.println("meet ArithmeticException,just resume");
            return SupervisorStrategy.resume();
        }else if(param instanceof  NullPointerException){
            System.out.println("meet NullPointerException,restart");
            return SupervisorStrategy.restart();
        }else if(param instanceof IllegalArgumentException){
            return SupervisorStrategy.stop();
        }
        else if(param instanceof SQLException){
            return SupervisorStrategy.restart();
        }else {
            return SupervisorStrategy.escalate();
        }
    }
}
