package com.tadobasolutions.service.Impl;

import com.tadobasolutions.entity.Counter;
import com.tadobasolutions.entity.Quote;
import com.tadobasolutions.repository.CounterRepository;
import com.tadobasolutions.repository.QuoteRepository;
import com.tadobasolutions.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final CounterRepository counterRepository;

    private static final String COUNTER_ID = "site_visit_counter";

    @Override
    public String saveQuote(Quote quote) {
        Quote newQuote = new Quote();
        newQuote.setCustomerName(quote.getCustomerName());
        newQuote.setCustomerEmail(quote.getCustomerEmail());
        newQuote.setCustomerMobileNumber(quote.getCustomerMobileNumber());
        newQuote.setQueryDescription(quote.getQueryDescription());
        Quote savedQuote = quoteRepository.save(newQuote);
        return "Quote saved with ID: " + savedQuote.getId();
    }

    @Override
    public List<Quote> getAllQuoteList() {
        return quoteRepository.findAll();
    }

    @Override
    public Long getVisitCount() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseGet(() -> {
                    Counter c = new Counter();
                    c.setId(COUNTER_ID);
                    c.setVisitCount(1L);
                    c.setLastUpdated(LocalDateTime.now());
                    return counterRepository.save(c);
                });
        return counter.getVisitCount();
    }

    @Override
    public void updateVisitCount() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseGet(() -> {
                    Counter c = new Counter();
                    c.setId(COUNTER_ID);
                    c.setVisitCount(0L);
                    c.setLastUpdated(LocalDateTime.now());
                    return counterRepository.save(c);
                });
        counter.setVisitCount(counter.getVisitCount() + 1);
        counter.setLastUpdated(LocalDateTime.now());
        counterRepository.save(counter);
    }
}
