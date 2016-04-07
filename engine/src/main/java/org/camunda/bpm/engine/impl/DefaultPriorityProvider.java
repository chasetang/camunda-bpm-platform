/*
 * Copyright 2016 camunda services GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.camunda.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

/**
 * Represents a default priority provider, which contains some functionality to evaluate the priority.
 * Can be used as base class for other priority providers. *  
 * 
 * @author Christopher Zell <christopher.zell@camunda.org>
 * @param <T> the type of the param to determine the priority
 */
public abstract class DefaultPriorityProvider<T> implements PriorityProvider<T> {
  
  /**
   * The default priority.
   */
  public static long DEFAULT_PRIORITY = 0;

  /**
   * The default priority in case of resolution failure.
   */
  public static long DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE = 0;
  
  /**
   * Returns the default priority.
   * 
   * @return the default priority
   */
  public long getDefaultPriority() {
    return DEFAULT_PRIORITY;
  }

  /**
   * Returns the default priority in case of resolution failure.
   * 
   * @return the default priority
   */
  public long getDefaultPriorityOnResolutionFailure() {
    return DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE;
  }
  
  /**
   * Evaluates a given value provider with the given execution entity to determine
   * the correct value. The error message heading is used for the error message 
   * if the validation fails because the value is no valid priority.
   * 
   * @param valueProvider the provider which contains the value
   * @param execution the execution entity
   * @param errorMessageHeading the heading which is used for the error message
   * @return the valid priority value
   */
  protected Long evaluateValueProvider(ParameterValueProvider valueProvider, ExecutionEntity execution, String errorMessageHeading) {
    Object value;
    try {
      value = valueProvider.getValue(execution);

    } catch (ProcessEngineException e) {

      if (Context.getProcessEngineConfiguration().isEnableGracefulDegradationOnContextSwitchFailure()
          && isSymptomOfContextSwitchFailure(e, execution)) {

        value = getDefaultPriorityOnResolutionFailure();
        logNotDeterminingPriority(execution, value, e);
      }
      else {
        throw e;
      }
    }

    if (!(value instanceof Number)) {
      throw new ProcessEngineException(errorMessageHeading + ": Priority value is not an Integer");
    }
    else {
      Number numberValue = (Number) value;
      if (isValidLongValue(numberValue)) {
        return numberValue.longValue();
      }
      else {
        throw new ProcessEngineException(errorMessageHeading + ": Priority value must be either Short, Integer, or Long");
      }
    }
  }
  
  /**
   * Logs the exception which was thrown if the priority can not be determined.
   * 
   * @param execution the current execution entity
   * @param value the current value
   * @param e the exception which was catched
   */
  protected abstract void logNotDeterminingPriority(ExecutionEntity execution, Object value, ProcessEngineException e);
  
  
  protected boolean isSymptomOfContextSwitchFailure(Throwable t, ExecutionEntity contextExecution) {
    // a context switch failure can occur, if the current engine has no PA registration for the deployment
    // subclasses may assert the actual throwable to narrow down the diagnose
    return ProcessApplicationContextUtil.getTargetProcessApplication(contextExecution) == null;
  }
  
  /**
   * Checks if the given number is a valid long value.
   * @param value the number which should be checked
   * @return true if is a valid long value, false otherwise
   */
  protected boolean isValidLongValue(Number value) {
    return
      value instanceof Short ||
      value instanceof Integer ||
      value instanceof Long;
  }
}