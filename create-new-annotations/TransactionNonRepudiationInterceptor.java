package com.im.imc.core.nonrepudiation.interceptor;


import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;

import com.im.imc.core.interceptor.AbstractMethodInterceptorWithIBOSessionData;
import com.im.imc.core.interceptor.InterceptorOrder;
import com.im.imc.core.model.ResultWithCode;
import com.im.imc.core.nonrepudiation.interceptor.applicable.INonRepudiationApplicable;
import com.im.imc.core.sessiondata.IMCBOSessionData;
import com.im.imc.nonrepudiation.annotation.NonRepudiationTransaction;
import com.im.imc.nonrepudiation.model.INonRepudiatable;
import com.im.imc.nonrepudiation.service.ITransactionNonRepudiationCRUDService;
import com.im.imc.nonrepudiation.validator.ITransactionNonRepudiationCRUDValidator;
import com.im.pcam.datatypes.authenticationMgr.IBOSessionData;


/**
 * Transaction Non Repudiation Intercepter, which intercepts any method annotated with {@link NonRepudiationTransaction}
 * 
 * @author zfattuhi
 */
public class TransactionNonRepudiationInterceptor extends AbstractMethodInterceptorWithIBOSessionData {


    public static final String RESULT_IS_NOT_INSTANCEOF_NON_REPUDIATABLE_ERROR_MESSAGE = "Result is not instanceof INonRepudiatable !";


    public static final String RESULT_IS_NULL_ERROR_MESSAGE = "Result is Null!";

    @Autowired
    private INonRepudiationApplicable nonRepudiationApplicable;


    /**
     * Transaction Non Repudiation service
     */
    @Autowired
    private ITransactionNonRepudiationCRUDService transactionNonRepudiationCRUDService;


    /**
     * Transaction non repudiation validation
     */
    @Autowired
    private ITransactionNonRepudiationCRUDValidator transactionNonRepudiationCRUDValidator;

    /**
     * Set Transaction Non Repudiation CRUD Service
     * 
     * @param transactionNonRepudiationCRUDService
     */
    public void setTransactionNonRepudiationCRUDService(ITransactionNonRepudiationCRUDService transactionNonRepudiationCRUDService) {
        this.transactionNonRepudiationCRUDService = transactionNonRepudiationCRUDService;
    }

    /**
     * Set Transaction Non Repudiation CRUD Validator
     * 
     * @param transactionNonRepudiationCRUDValidator
     */
    public void setTransactionNonRepudiationCRUDValidator(ITransactionNonRepudiationCRUDValidator transactionNonRepudiationCRUDValidator) {
        this.transactionNonRepudiationCRUDValidator = transactionNonRepudiationCRUDValidator;
    }

    /**
     * Set NonRepudiationApplicable Instance.
     * @param nonRepudiationApplicable
     */
    public void setNonRepudiationApplicable(INonRepudiationApplicable nonRepudiationApplicable) {
        this.nonRepudiationApplicable = nonRepudiationApplicable;
    }

    /**
     * Invoke method
     */
    @SuppressWarnings("rawtypes")
    public Object invoke(MethodInvocation invocation, IBOSessionData sessionData) throws Throwable {

        IMCBOSessionData imcBOSessionData = (IMCBOSessionData) sessionData;

        if (!nonRepudiationApplicable.isNonRepudiationApplicable(imcBOSessionData)) {
            return invocation.proceed();
        }
        // Verify the message and the signature
        transactionNonRepudiationCRUDValidator.validateNonRepudiationMessageSignature(imcBOSessionData.getNonRepudiationMessageSignature(),
                imcBOSessionData);

        // Invoke the original method
        Object result = invocation.proceed();

        if (result == null) {
            // Result is null
            throw new NullPointerException(RESULT_IS_NULL_ERROR_MESSAGE);
        }
        if (result instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<?> nonRepudiatableList = (List<INonRepudiatable>) result;
            for (Object value : nonRepudiatableList) {
                // Insert non-repudiation record
                insertNonRepudiationRecord(value, imcBOSessionData);
            }
        } else if (result instanceof ResultWithCode) {
            // Result is instance of ResultWithCode, so extract the original result and pass its non-repudiation data.
            ResultWithCode resultWithCode = (ResultWithCode) result;
            insertNonRepudiationRecord(resultWithCode.getResult(), imcBOSessionData);
        } else {
            // Insert non-repudiation record
            insertNonRepudiationRecord(result, imcBOSessionData);
        }

        // Wipe out the existing message and signature, so that it won't be reused again
        imcBOSessionData.setNonRepudiationMessageSignature(null);
        return result;
    }

    /**
     * Insert Non-Repudiation Record.
     * 
     * @param value
     * @param imcBOSessionData
     * @throws Throwable
     */
    private void insertNonRepudiationRecord(Object value, IMCBOSessionData imcBOSessionData) throws Throwable {
        if (value instanceof INonRepudiatable) {
            // Cast to INonRepudiatable interface.
            INonRepudiatable nonRepudiatable = (INonRepudiatable) value;
            // Insert
            transactionNonRepudiationCRUDService.insert(nonRepudiatable, imcBOSessionData);
        } else {
            throw new ClassCastException(RESULT_IS_NOT_INSTANCEOF_NON_REPUDIATABLE_ERROR_MESSAGE);
        }
    }

    public int getOrder() {
        return InterceptorOrder.NON_REPUDIATION_INTERCEPTOR_ORDER;
    }


}
