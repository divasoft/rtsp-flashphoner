# Divasoft RTSP Tester 1.0
Средство тестирования rtsp потоков.

Собирает данные:
* Пинг до камеры
* Порт rtsp потока
* Порт админки камеры (80)
* Открытие по http протоколу админки камеры и получение заголовка страницы
* Один кадр видеопотока
* Время получения кадра
* Лог от ffmpeg

Источник данных:
* файл на локальном сервере
* ссылка на удалённом сервере (http)
* единичная ссылка

Запуск
```java -jar rtsp.jar```

Использование
```
 -D,--debug          Debug ON
 -F,--file <arg>     File path to list rtsp
 -H,--help           Help info
 -S,--single <arg>   RTSP single url
 -U,--url <arg>      URL to list rtsp
```

На сервере должна быть установлена минимум **Java 8**
```sudo apt install default-jre```

На сервере должен быть установлен **ffmpeg**
```sudo apt install ffmpeg```

Команды веб-сервера
```
/start # Запустить проверку
/status # Вывести статус проверки
/report # Вывести сформированных xml отчёт
/log # Показать общий лог
/flashphoner/ssl # Проверить дату сертификата
/flashphoner/service # Вывести информацию по службам/портам
```
При первом запуске генерируется конфигурационный файл
```ini
[main]
PING_TIMEOUT = 1000 # Время ожидания пинга
PORT_TIMEOUT = 1000 # Время ожидания проверки порта
PROCESS_TIMEOUT = 30000 # Время ожидания изображения для ffmpeg
WEB_CLIENT_TIMEOUT = 1000 # Таймаут эмулятора браузера при подключении к админке регистратора/камеры
THREADS_COUNT = 12 # Количество потоков = количество ядер
TRY_GET_IMAGE_CNT = 2 # Количество попыток получить изображение с камеры/регистратора
TRY_GET_IMAGE_TIME = 5000 # Интервал между попытками

[web]
INTERNAL_WEB_SERVER_HOST = 127.0.0.1 # На каком адресе запустить встроенный веб-сервер для обработки команд
INTERNAL_WEB_SERVER_PORT = 8081 # Если порт >0, запускается веб-сервер
INTERNAL_WEB_SERVER_LOGIN = admin # Логин для basic http authentication
INTERNAL_WEB_SERVER_PWD = admin # Пароль для basic http authentication

[flashphoner]
FLASHPHONER_URL = https://video.divasoft.ru # Адрес где расположен Flashphoner
FLASHPHONER_SERVICES = webcallserver|haproxy|turnserver # Имена служб для проверки статуса
FLASHPHONER_PORTS = 1935|3478|5349|80|8080|8081|8082|443|8443|8444|8445|2000|9091|8888 # Проверка этого набора портов
```

Используемые библиотеки
* HtmlUnit - Эмулятор браузера
* ApacheCli - Обработка командной строки
* ini4j - Работа с ini файлами

Пример отчёта
```xml
<?xml version="1.0" encoding="UTF-8"?>
<report date="17-12-2019 11:05:39" items="2">
    <item admin_title="" image_log="ffmpeg version 3.4.6-0ubuntu0.18.04.1 Copyright (c) 2000-2019 the FFmpeg ..." host="192.168.0.12" is_admin_port="Y" is_image="N" is_ping="Y" is_port="Y" port="554" time="4667" url="rtsp://192.168.0.12:554/user=admin&amp;password=admin&amp;channel=1&amp;stream=1?.sdp"/>
    <item admin_title="Login" image_log="ffmpeg version 3.4.6-0ubuntu0.18.04.1 Copyright (c) 2000-2019 the FFmpeg ..." host="192.168.0.15" is_admin_port="Y" is_image="Y" is_ping="Y" is_port="Y" port="554" time="6417" url="rtsp://192.168.0.15:554/user=admin&amp;password=admin&amp;channel=1&amp;stream=1?.sdp"/>Вот тут будет содержимое изображения в base64</item>
```

ps. Всё это добро прикручивается к админке Битрикса, там уже дополняем дополнительной проверкой - запуск каждого потока, получение изображения средствами flashphoner-web-api. И формируем человекочитаемый отчёт по камерам.