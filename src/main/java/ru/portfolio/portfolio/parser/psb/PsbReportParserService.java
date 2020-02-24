package ru.portfolio.portfolio.parser.psb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.controller.EventCashFlowRestController;
import ru.portfolio.portfolio.controller.SecurityRestController;
import ru.portfolio.portfolio.controller.TransactionCashFlowRestController;
import ru.portfolio.portfolio.controller.TransactionRestController;
import ru.portfolio.portfolio.pojo.*;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class PsbReportParserService {
    private final SecurityRestController securityRestController;
    private final EventCashFlowRestController eventCashFlowRestController;
    private final TransactionRestController transactionRestController;
    private final TransactionCashFlowRestController transactionCashFlowRestController;

    public void parse(String reportFile) {
        try (PsbBrokerReport report = new PsbBrokerReport(reportFile)) {
            //CashTable cashTable = new CashTable(report);
            CashFlowTable cashFlowTable = new CashFlowTable(report);
            PortfolioTable portfolioTable = new PortfolioTable(report);
            TransactionTable transactionTable = new TransactionTable(report);
            CouponAndAmortizationTable couponAndAmortizationTable = new CouponAndAmortizationTable(report);
            DividendTable dividendTable = new DividendTable(report);
            DerivativeTransactionTable derivativeTransactionTable = new DerivativeTransactionTable(report);
            DerivativeCashFlowTable derivativeCashFlowTable = new DerivativeCashFlowTable(report);

            addSecurities(portfolioTable);
            addCashInAndOutFlows(cashFlowTable);
            addTransaction(transactionTable);
            addCouponAndAmortizationCashFlows(couponAndAmortizationTable);
            addDividendCashFlows(dividendTable);
            addDerivativeTransaction(derivativeTransactionTable);
            addDerivativeCashFlows(derivativeCashFlowTable);
        } catch (Exception e) {
            log.warn("Не могу открыть/закрыть отчет {}", reportFile, e);
        }
    }

    private void addSecurities(PortfolioTable portfolioTable) {
        for (PortfolioTable.Row row : portfolioTable.getData()) {
            addSecurity(row.getIsin(), row.getName());
        }
    }

    private void addCashInAndOutFlows(CashFlowTable cashFlowTable) {
        for (CashFlowTable.Row row : cashFlowTable.getData()) {
            addEventCashFlow(EventCashFlow.builder()
                    .eventType(row.getType())
                    .timestamp(row.getTimestamp())
                    .value(row.getValue())
                    .currency(row.getCurrency()));
        }
    }

    private void addTransaction(TransactionTable transactionTable) {
        for (TransactionTable.Row row : transactionTable.getData()) {
            try {
                boolean isAdded = addTransaction(Transaction.builder()
                        .id(row.getTransactionId())
                        .isin(row.getIsin())
                        .timestamp(row.getTimestamp())
                        .count(row.getCount()));
                if (isAdded) {
                    TransactionCashFlow cashFlow = TransactionCashFlow.builder()
                            .transactionId(row.getTransactionId())
                            .currency(row.getCurrency())
                            .build();
                    if (!row.getValue().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowEvent.PRICE)
                                .value(row.getValue()));
                    }
                    if (!row.getAccruedInterest().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowEvent.ACCRUED_INTEREST)
                                .value(row.getAccruedInterest()));
                    }
                    if (!row.getCommission().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowEvent.COMMISSION)
                                .value(row.getCommission()));
                    }
                }
            } catch (Exception e) {
                log.warn("Не могу добавить транзакцию {}", row, e);
            }
        }
    }

    private void addCouponAndAmortizationCashFlows(CouponAndAmortizationTable couponAndAmortizationTable) {
        for (CouponAndAmortizationTable.Row row : couponAndAmortizationTable.getData()) {
            addSecurity(row.getIsin()); // required for amortization
            addEventCashFlow(EventCashFlow.builder()
                    .isin(row.getIsin())
                    .count(row.getCount())
                    .eventType(row.getEvent())
                    .timestamp(row.getTimestamp())
                    .value(row.getValue())
                    .currency(row.getCurrency()));
        }
    }

    private void addDividendCashFlows(DividendTable dividendTable) {
        for (DividendTable.Row row : dividendTable.getData()) {
            addEventCashFlow(EventCashFlow.builder()
                    .isin(row.getIsin())
                    .count(row.getCount())
                    .eventType(row.getEvent())
                    .timestamp(row.getTimestamp())
                    .value(row.getValue())
                    .currency(row.getCurrency()));
        }
    }

    private void addDerivativeTransaction(DerivativeTransactionTable derivativeTransactionTable) {
        for (DerivativeTransactionTable.Row row : derivativeTransactionTable.getData()) {
            addSecurity(row.getIsin());
            try {
                boolean isAdded = addTransaction(Transaction.builder()
                        .id(row.getTransactionId())
                        .isin(row.getIsin())
                        .timestamp(row.getTimestamp())
                        .count(row.getCount()));
                if (isAdded) {
                    TransactionCashFlow cashFlow = TransactionCashFlow.builder()
                            .transactionId(row.getTransactionId())
                            .currency(row.getCurrency())
                            .build();
                    if (!row.getValue().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowEvent.DERIVATIVE_PRICE)
                                .value(row.getValue()));
                    }
                    if (!row.getCommission().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowEvent.COMMISSION)
                                .value(row.getCommission()));
                    }
                }
            } catch (Exception e) {
                log.warn("Не могу добавить транзакцию {}", row, e);
            }
        }
    }

    private void addDerivativeCashFlows(DerivativeCashFlowTable derivativeCashFlowTable) {
        for (DerivativeCashFlowTable.Row row : derivativeCashFlowTable.getData()) {
            if (row.getContract() != null) {
                boolean isAdded = addSecurity(row.getContract());
                if (!isAdded) continue;
            }
            addEventCashFlow(EventCashFlow.builder()
                    .isin(row.getContract())
                    .count(row.getCount())
                    .eventType(row.getEvent())
                    .timestamp(row.getTimestamp())
                    .value(row.getValue())
                    .currency(row.getCurrency()));
        }
    }

    private boolean addSecurity(String isin) {
        return addSecurity(isin, null);
    }

    private boolean addSecurity(String isin, String name) {
        return addSecurity(Security.builder()
                .isin(isin)
                .name(name));
    }

    private boolean addSecurity(Security.SecurityBuilder security) {
        try {
            HttpStatus status = securityRestController.post(security.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить ЦБ {} в список", security);
                return false;
            }
        } catch (Exception e) {
            log.warn("Не могу добавить ЦБ {} в список", security, e);
            return false;
        }
        return true;
    }

    private boolean addTransaction(Transaction.TransactionBuilder transaction) {
        try {
            HttpStatus status = transactionRestController.post(transaction.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить транзакцию {}", transaction);
                return false;
            }
        } catch (Exception e) {
            log.warn("Не могу добавить транзакцию {}", transaction, e);
            return false;
        }
        return true;
    }

    private void addTransactionCashFlow(TransactionCashFlow.TransactionCashFlowBuilder transactionCashFlow) {
        try {
            HttpStatus status = transactionCashFlowRestController.post(transactionCashFlow.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow);
            }
        } catch (Exception e) {
            log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow, e);
        }
    }

    private void addEventCashFlow(EventCashFlow.EventCashFlowBuilder eventCashFlow) {
        try {
            HttpStatus status = eventCashFlowRestController.post(eventCashFlow.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о движении денежных средств {}", eventCashFlow);
            }
        } catch (Exception e) {
            log.warn("Не могу добавить информацию о движении денежных средств {}", eventCashFlow, e);
        }
    }
}
