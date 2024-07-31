package com.javax0.axsessgard.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("/")
class HomeController {

    @GetMapping
    fun index(): ModelAndView {
        // Return the view name of your HTML file
        return ModelAndView("test.html")
    }
}
