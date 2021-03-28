/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.investbook.converter.ForeignExchangeRateConverter;
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.repository.ForeignExchangeRateRepository;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class CbrForeignExchangeRateService {
    private final UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl("https://www.cbr.ru/Queries/UniDbQuery/DownloadExcel/98956")
            .queryParam("Posted", true)
            .queryParam("mode", 1)
            .queryParam("ToDate", "01/01/2100");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final Map<String, ?> currencyParamValues = Map.of(
            "USD", "R01235",
            "EUR", "R01239",
            "GBP", "R01035",
            "CHF", "R01775");
    private final ForeignExchangeRateRepository foreignExchangeRateRepository;
    private final ForeignExchangeRateConverter foreignExchangeRateConverter;
    private final RestTemplate restTemplate;

    @Transactional
    public void updateFrom(LocalDate fromDate) {
        try {
            String formattedFromDate = fromDate.format(dateTimeFormatter);
            for (var e : currencyParamValues.entrySet()) {
                long t0 = System.nanoTime();
                String currency = e.getKey();
                String currencyPair = currency.toUpperCase() + ForeignExchangeRateService.RUB;
                URI uri = this.uri
                        .cloneBuilder()
                        .queryParam("VAL_NM_RQ", e.getValue())
                        .queryParam("FromDate", formattedFromDate)
                        .build()
                        .toUri();
                Resource resource = restTemplate.getForObject(uri, Resource.class);
                updateBy(resource, currencyPair, uri);
                log.info("Курс {} обновлен за {}", currencyPair, Duration.ofNanos(System.nanoTime() - t0));
            }
        } catch (Exception e) {
            throw new RuntimeException("Не смог обновить курсы валют", e);
        }
    }

    private void updateBy(Resource resource, String currencyPair, URI uri) throws IOException {
        Objects.requireNonNull(resource, () -> "Не удалось скачать курсы валют с адреса " + uri);
        Workbook book = new XSSFWorkbook(resource.getInputStream());
        new ExcelSheet(book.getSheetAt(0))
                .createOfNoName("data", TableHeader.class)
                .stream()
                .map(row -> getRate(row, currencyPair))
                .forEach(this::save);
    }

    private static ForeignExchangeRate getRate(TableRow row, String currencyPair) {
        return ForeignExchangeRate.builder()
                .date(row.getLocalDateTimeCellValue(TableHeader.DATE).toLocalDate())
                .currencyPair(currencyPair)
                .rate(row.getBigDecimalCellValue(TableHeader.FX_RATE))
                .build();
    }

    private void save(ForeignExchangeRate fxRate) {
        try {
            ForeignExchangeRateEntity entity = foreignExchangeRateConverter.toEntity(fxRate);
            foreignExchangeRateRepository.save(entity);
        } catch (Exception e) {
            log.debug("Ошибка сохранения {}, может быть запись уже существует?", fxRate);
        }
    }

    @RequiredArgsConstructor
    public enum TableHeader implements TableColumnDescription {
        DATE(TableColumnImpl.of("data")),
        FX_RATE(TableColumnImpl.of("curs"));

        @Getter
        private final TableColumn column;
    }
}
