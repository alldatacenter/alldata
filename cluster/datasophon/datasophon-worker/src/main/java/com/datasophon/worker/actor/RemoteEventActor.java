package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import akka.remote.AssociatedEvent;
import akka.remote.AssociationErrorEvent;
import akka.remote.DisassociatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteEventActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(RemoteEventActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if(msg instanceof AssociationErrorEvent){
            AssociationErrorEvent aee = (AssociationErrorEvent) msg;
            logger.info(aee.getLocalAddress()+"-->"+aee.getRemoteAddress()+": "+aee.getCause());
        }else if(msg instanceof AssociatedEvent){
            AssociatedEvent ae = (AssociatedEvent) msg;
            logger.info(ae.getLocalAddress()+"-->"+ae.getRemoteAddress()+" associated");
        }else if(msg instanceof DisassociatedEvent){
            DisassociatedEvent de = (DisassociatedEvent) msg;
            logger.info(de.getLocalAddress()+"-->"+de.getRemoteAddress()+" disassociated");
        }
    }
}
