package org.drools.gorm.impl;

import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;

public class ProcessEventListenerAdapter implements ProcessEventListener {

    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent event) {

    }

    @Override
    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {

    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {

    }

    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {

    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {

    }

    @Override
    public void beforeNodeLeft(ProcessNodeLeftEvent event) {

    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {

    }

    @Override
    public void beforeProcessCompleted(ProcessCompletedEvent event) {

    }

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {

    }

    @Override
    public void beforeVariableChanged(ProcessVariableChangedEvent event) {

    }
}
