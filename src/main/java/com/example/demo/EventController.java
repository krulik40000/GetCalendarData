package com.example.demo;


import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@RestController
public class EventController {



    @RequestMapping("/getweeiacalendar")
    public Object calendar(@RequestParam(defaultValue = "2020") int year, @RequestParam(defaultValue = "01") int month) {
        String LINK = "http://weeia.p.lodz.pl/pliki_strony_kontroler/kalendarz.php";
        String YEARPAR = "?rok=";
        String MONTHPAR = "&miesiac=";
        String HTMLSELECTOR = "td";
        String CLASSNAME = "active";
        String MATCHER = "a class=\"active\" href=\"javascript:void();\"";
        String REGEX = "\\n";
        String FILE = "calendar.ics";
        String MEDIATYPE = "text/calendar";
        int INDEX = 0;

        String url = LINK + YEARPAR + year + MONTHPAR + month;
        List<DayEvent> calendarEvents = new ArrayList<>();
        ICalendar calendar = new ICalendar();
        LocalDate localDate = LocalDate.now();
        Month actualMonth = localDate.getMonth();

        try {
            Document HtmlDocument = Jsoup.connect(url).get();
            for (Element tdElement : HtmlDocument.select(HTMLSELECTOR)) {
                for (Element activeClassElement : tdElement.getElementsByClass(CLASSNAME)) {
                    if (activeClassElement.toString().contains(MATCHER)) {
                        String[] splitted = activeClassElement.toString().split(REGEX);
                        String day = splitted[INDEX].substring(splitted[INDEX].lastIndexOf("\">") + 2, splitted[INDEX].lastIndexOf("<"));
                        for (Element pElement : activeClassElement.getElementsByTag("p")) {
                            String pString = pElement.toString();
                            String event = pString.substring(pString.indexOf(">") + 1, pString.lastIndexOf("<"));
                            calendarEvents.add(new DayEvent(day, event));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (DayEvent calendarEvent : calendarEvents) {
            VEvent event = new VEvent();
            event.setSummary(calendarEvent.getEventName());
            Date eventDate = new GregorianCalendar(localDate.getYear(), actualMonth.getValue(), Integer.parseInt(calendarEvent.getDay()) + 1).getTime();
            event.setDateStart(eventDate);
            event.setDateEnd(eventDate);
            calendar.addEvent(event);
        }

        File calendarFile = new File(FILE);
        try {
            Biweekly.write(calendar).go(calendarFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Resource fileSystemResource = new FileSystemResource(calendarFile);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MEDIATYPE))
                .body(fileSystemResource);
    }
}
