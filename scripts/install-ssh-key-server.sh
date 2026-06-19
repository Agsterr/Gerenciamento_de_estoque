#!/bin/bash
# Cole este comando no terminal SSH já conectado ao servidor (como root)

mkdir -p ~/.ssh && chmod 700 ~/.ssh
cat >> ~/.ssh/authorized_keys << 'EOF'
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDGVAc7L5y/o5ieQdcVGoK4Nj4VX2bk9aKhO82u8SRZ4ZQmgRkNVtAsVVeiqlo4aEPkRuZo1qjccSkvVgn0LpvYW5A/epOAifb6OwHJNHgjbc42EFn7RF+Y/xQjpTlbG0XN2DLh3a0BM/Lav5Hs1GbQlIjmBPs6pigghP8E4lFNJRz/ep5koJLjF51mzZdpXq8G6s/l5Uc39yYcTwhwfMQkrfdyq+IutT96U3zBuVEMgMmO02CJdPQ1nGICxmh/kWVeU5RUSpXBMVds/YvAXA7fnbw4GuesBo3QRxqwS+JjPGGt2Tr5h704nvul21GnjMrqaRXMGvNjzUL9v1KrA/r90QEBKhHqO6P/ilxY/YoZL0+p9tXwqhcZZOgX28m7fQY+BqIyNvB6mbUdWM2be46azmEkyC8n3CuHjwNmw8OPI887WxnUNxynoCs7KzvWSC89qnSOXpremNdwP9/Y+lAFJQ12w07rHZwZwZbPvXThNkF8aLwkPcaKwpwRhtnik/K25LTTlBsf/8sgUC2/ncfCmXbd6yKD+wSoDert0vUfK0xwXHql9Ih76laf78sTsycXpKbd+uTeLID1pe4DBqUI21cGWOZJihg/e9Lhx4GhlthkX5uDMUUwbVlMUj8H73zlhJep8DK4nME2ekyRhWz2TC0OyoJUUP4y0k5PgtBdGQ== agster.santos01@gmail.com
EOF
chmod 600 ~/.ssh/authorized_keys
echo "Chave SSH instalada com sucesso!"
