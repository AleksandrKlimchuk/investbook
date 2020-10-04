/*
 * InvestBook
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

package ru.investbook.parser.vtb;

import lombok.Getter;
import ru.investbook.parser.*;
import ru.investbook.parser.table.Table;
import ru.investbook.parser.table.TableRow;
import ru.investbook.pojo.EventCashFlow;

import java.util.Collection;
import java.util.Collections;

public class VtbCashFlowTable extends AbstractReportTable<EventCashFlow> {

    private static final String TABLE_NAME = "Движение денежных средств";

    public VtbCashFlowTable(BrokerReport report) {
        super(report, TABLE_NAME, null, VtbCashFlowTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> getRow(Table table, TableRow row) {
        return Collections.emptyList();
    }

    @Getter
    private enum VtbCashFlowTableHeader implements TableColumnDescription {
        DATE("дата"),
        VALUE("сумма"),
        CURRENCY("валюта"),
        TYPE("тип операции"),
        DESCRIPTION("комментарий");

        private final TableColumn column;

        VtbCashFlowTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
