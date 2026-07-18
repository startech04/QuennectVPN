package com.quennect.vpn

object QuennectConfig {
    /**
     * Matching Netmod's configuration logic exactly.
     * address: firebase-settings.crashlytics.com
     * host (inside WS): vless-proxy-373791452353.asia-southeast1.run.app
     * sni: firebase-settings.crashlytics.com
     */
    val vlessJsonConfig = """
    {
      "log": {
        "access": "",
        "error": "",
        "loglevel": "warning"
      },
      "dns": {
        "servers": [
          "1.1.1.1",
          "1.0.0.1"
        ]
      },
      "inbounds": [
        {
          "tag": "tun-in",
          "protocol": "tun",
          "settings": {
            "name": "tun0",
            "mtu": 1500,
            "sniffing": {
              "enabled": true,
              "destOverride": ["http", "tls", "quic"]
            }
          }
        }
      ],
      "outbounds": [
        {
          "protocol": "vless",
          "settings": {
            "vnext": [
              {
                "address": "firebase-settings.crashlytics.com",
                "port": 443,
                "users": [
                  {
                    "id": "1b49159b-e3af-4e81-b6d3-8041e8935d7d",
                    "encryption": "none",
                    "level": 0
                  }
                ]
              }
            ]
          },
          "streamSettings": {
            "network": "ws",
            "security": "tls",
            "tlsSettings": {
              "serverName": "firebase-settings.crashlytics.com",
              "allowInsecure": false
            },
            "wsSettings": {
              "path": "/Telegram/@Private_Vpn_Tunnel",
              "headers": {
                "Host": "vless-proxy-373791452353.asia-southeast1.run.app"
              }
            }
          },
          "tag": "proxy"
        },
        {
          "protocol": "freedom",
          "tag": "direct",
          "settings": {}
        }
      ],
      "routing": {
        "domainStrategy": "AsIs",
        "rules": [
          {
            "type": "field",
            "inboundTag": ["tun-in"],
            "outboundTag": "proxy"
          },
          {
            "type": "field",
            "port": 53,
            "outboundTag": "proxy"
          }
        ]
      }
    }
    """.trimIndent()
}
