/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Links securities events (dividends, bond amortizations) with transactions.
 */
@Getter
public class PaidInterest {
    private final HashMap<CashFlowType, Map<Position, List<SecurityEventCashFlow>>> paidInterest = new HashMap<>();

    Map<Position, List<SecurityEventCashFlow>> get(CashFlowType type) {
        return paidInterest.computeIfAbsent(type, k -> new HashMap<>());
    }

    public List<SecurityEventCashFlow> get(CashFlowType payType, Position position) {
        List<SecurityEventCashFlow> value = this.get(payType).get(position);
        return (value != null) ? value : Collections.emptyList();
    }
}
