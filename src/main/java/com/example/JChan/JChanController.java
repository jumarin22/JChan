package com.example.JChan;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller  // This annotation indicates that this class serves as a controller in the MVC pattern
public class JChanController {

    private static final Logger logger = LoggerFactory.getLogger(JChanController.class);  // Logger instance for logging

    @GetMapping("/fetch")  // This annotation maps HTTP GET requests onto this method
    public ModelAndView fetchPost(@RequestParam String url) throws Exception {
        Document doc = Jsoup.connect(url).get();  // Connect to the URL and get the HTML document

        // Extract all CSS links from the HTML document
        Elements linkElements = doc.select("link[rel=stylesheet]");
        List<String> cssLinks = new ArrayList<>();
        for (Element link : linkElements) {
            cssLinks.add(link.absUrl("href"));
        }

        // Extract the original post and all replies from the HTML document
        Element postOp = doc.select("div.post.op").first();
        Elements postReplies = doc.select("div.post.reply");

        // Regular expression pattern for 9-digit numbers
        Pattern pattern = Pattern.compile("(\\d{9})");
        Map<String, Integer> countMap = new HashMap<>();

        // Extract all replies and count the occurrences of 9-digit numbers in each reply
        List<Reply> replyList = new ArrayList<>();
        for (Element postReply : postReplies) {
            String replyId = postReply.id();
            String replyHtml = postReply.html();
            replyList.add(new Reply(replyId, replyHtml));

            Matcher matcher = pattern.matcher(replyHtml);
            while (matcher.find()) {
                String match = matcher.group();
                countMap.put(match, countMap.getOrDefault(match, 0) + 1);
            }
        }

        // Sort the entries of countMap in descending order of their values (counts)
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(countMap.entrySet());
        entryList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        // Create a list called "topReplies" and add the keys (9-digit numbers) of the top 5 entries to this list
        List<String> topReplies = new ArrayList<>();
        int limit = Math.min(5, entryList.size());
        for (int i = 0; i < limit; i++) {
            topReplies.add(entryList.get(i).getKey());
        }

        // Create a BiFunction for checking if a string contains any of the elements in a list
        BiFunction<String, List<String>, Boolean> containsAnyFunction = this::containsAny;

        // Create a map where the keys are the top 5 numbers and the values are the corresponding replies
        Map<String, Reply> topRepliesMap = new LinkedHashMap<>();
        for (String number : topReplies) {
            for (Reply reply : replyList) {
                if (reply.getId().contains(number)) {
                    topRepliesMap.put(number, reply);
                    break;
                }
            }
        }

        // Create a ModelAndView object and add all necessary objects to it
        ModelAndView modelAndView = new ModelAndView("post");
        modelAndView.addObject("opContent", postOp.html());
        modelAndView.addObject("replyList", replyList);
        modelAndView.addObject("cssLinks", cssLinks);
        modelAndView.addObject("topReplies", topReplies);
        modelAndView.addObject("containsAny", containsAnyFunction);
        modelAndView.addObject("topRepliesMap", topRepliesMap);

        // For debugging purposes, log the top 5 numbers and their counts
        for (String number : topReplies) {
            logger.info("Number: " + number + ", Count: " + countMap.get(number));
        }

        return modelAndView;  // Return the ModelAndView object
    }

    // This method checks if a string contains any of the elements in a list
    public boolean containsAny(String str, List<String> elements) {
        for (String element : elements) {
            if (str.contains(element)) {
                return true;
            }
        }
        return false;
    }
}