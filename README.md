webutilities
============

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.googlecode.webutilities/webutilities/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.googlecode.webutilities/webutilities/)
[![License](https://img.shields.io/badge/license-Apache_2-blue.svg?style=flat)](https://github.com/rpatil26/webutilities/blob/master/LICENSE)

Java library that helps you to speed up and improve client side performance of your JEE web application.

### Introduction

Client side performance is important for better user experience. Optimizing and efficiently serving the static resources (JS, HTML, CSS, Images etc.) significantly increases client side performance. This Java Library (WebUtilities) provides web components to help you speed up Front-End of your J2EE application.

It is said that 80% of the end-user response time is spent on the front-end. To make the front-end efficient and friendly to the browser, various performance practices have been suggested. We can measure page performance using tools such as YSlow or Page Speed. These tools validate page against the best practices and give the performance ratings/grade. WebUtilities provides inbuilt components to apply some of those best practices in your web application with minimal change to speed it up and get higher performance score. Below screenshots shows the difference it makes.

### Features

*   Serve multiple JS or CSS files in one request
*   Add Expires header for JS, CSS and Image files to be cached by browser
*   Minify JS, CSS files on the fly
*   Minify Inline CSS and JS code blocks
*   Add Character Encoding to your response
*   Server compressed contents (gzip/compress/deflate)
*   Cache responses to speed loading by avoiding reprocessing Let's see how to apply those best practices in your application and speed it up :)

### Get started

*   Refer Wiki for documentation
*   Configure your web.xml with chain of filters accordingly. For Examples and step by step Guide, Visit Wiki. For any issues or have feature suggestions, report them here.

### Dependencies

WebUtilities uses Maven to manage the dependencies. Please refer POM details for the list of dependencies.

