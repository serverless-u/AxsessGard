package com.javax0.axsessgard.utils

const val AXSG_CONFIG_DIR = "AXSG_CONFIG_DIR"

class Configuration {

    companion object {
        val DIRECTORY: String =
            System.getenv(AXSG_CONFIG_DIR) ?: System.getProperty(AXSG_CONFIG_DIR) ?: "/etc/axsessgard"
    }

}