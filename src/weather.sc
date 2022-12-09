theme: /Weather 
    
    state: Begin
    #вопрос из любого места о погоде
        intent!: /Погода
    #если в запросе найдена сущность Дата - сохраняем ее в сессионную переменную
        if: $parseTree.Date
            script: $session.date = $parseTree.Date[0].value
    #если в запросе найдена сущность Место - сверяем со справочниками Города и Страны
        if: $parseTree.Geo
            script: 
                $temp.geo = $parseTree.Geo[0].value;
                if ( $nlp.matchPatterns($temp.geo, ["$City"]) )
                    $session.geo = $nlp.matchPatterns($temp.geo, ["$City"]).parseTree
                else 
                    if ( $nlp.matchPatterns($temp.geo, ["$Country"]) )
                        $session.geo = $nlp.matchPatterns($temp.geo, ["$Country"]).parseTree
                    else $session.geo = ""
    #если Место совпало с данными справочников - сохраняем название и координаты
                if ( $session.geo ) getLocation ($session.geo)
    #если город/страна уже записаны в сессионные переменные - идем на запрос даты
        if: $session.place
            go!: /Weather/Step2
        #если нет ни города, ни страны - запрашиваем
        else:    
            a: Где смотрим погоду? Назовите город или страну
    
    #если назвали город или страну - записали их и идем на Шаг 2 заявки   
        state: Location
            q: * $City *
            q: * $Country *
            script: getLocation ($parseTree)
            go!: /Weather/Step2
    #если отказ
        state: Deny
            q: ( * (не надо)/(не хочу)/(не буду) * )
            q: $refuse
            random:
                a: Как скажете
                a: Хорошо
            go!: /Exit
    #если название непонятное - переспрашиваем и идем на начало чтоб спросить страну или город
        state: NoMatch
            event: noMatch
            random:
                a: Простите, я не знаю такого названия
                a: Это местоположение мне незнакомо
            go!: /Weather/Begin
            
    #запрос даты
    state: Step2
        #если дата уже была раньше сохранена - идем на Шаг3 погоды
        if: $session.date
            go!: /Weather/Step3
    #иначе - спрашиваем дату
        else:
            random:
                a: На какую дату смотрим прогноз погоды?
                a: Какая дата Вам интересна для прогноза погоды?
    #названа дата - сохраняем ее и идем на Шаг3 погоды
        state: Date
            q: * @duckling.date *
            script: $session.date = $parseTree.value
            go!: /Weather/Step3
    #отказ - идем на выход
        state: Deny
            q: ( * (не надо)/(не хочу)/(не буду) * )
            q: $refuse
            random:
                a: Как скажете
                a: Погода внезапно испортилась сегодня!
            go!: /Exit
    #любой другой ответ - подсказываем что надо ответить и идем на начало шага
        state: NoMatch
            event: noMatch
            random:
                a: Не похоже на дату. Пожалуйста, назовите число и месяц
                a: Вы назвали некорректную дату, повторите пожалуйста
            go!: /Weather/Step2    
    
    #проверка даты
    state: Step3
    #вызвали текущую дату, сравнили ее с сохраненной, определили интервал прогноза
        script:
            $temp.nowDate = Date.parse ($jsapi.dateForZone ("Europe/Moscow","yyyy-MM-dd"));
            $session.interval = ($session.date.timestamp - $temp.nowDate)/60000/60/24;
    #если интервал в рамках границ прогноза - идем его запрашивать
        if: ( $session.interval == 0 || ($session.interval > 0 && $session.interval < 17) )
            go!: /Weather/ForecastStep4
    #если нет - уточняем у пользователя что он хочет
        else: 
            script: $temp.answerDate = $session.date.day + " " + months[$session.date.month].name; 
            if: ( $session.interval == 17 ||  $session.interval > 17 )
                a: Прогноз у меня только на 16 дней. Узнать, какая была погода {{$temp.answerDate}} прошлого года?
                buttons:
                    "Да, узнать"
                    "Нет, не надо"
            else:    
                a: Эта дата в прошлом. Узнать, какая была погода {{$temp.answerDate}} прошлого года?
                buttons:
                    "Да, узнать"
                    "Нет, не надо"
        
    #если да - идем запрашивать исторические данные
        state: Yes
            q: $yes
            q: (* узнай/посмотри *)
            go!: /Weather/HistoryStep4
    #если нет - очистили дату и идем снова спрашивать дату
        state: No
            q: $no
            q: (* другая/другую [~дата] *)
            script: delete $session.date
            a: Хорошо.
            go!: /Weather/Step2
        
    #функция запроса погоды
    state: ForecastStep4
        script:
    #запрашиваем погоду по API и сохраняем температуру
            $temp.weather = getWeather($session.coordinates.lat, $session.coordinates.lon, $session.interval);
            $session.temperature = $temp.weather.temp;
    #если ответ пришел - выдаем его
        if: $temp.weather
            script: 
    #формируем часть ответа про день/дату, на который получен прогноз
                if ($session.interval == 0) $temp.answerDate = "сегодня";
                    else if ($session.interval == 1) $temp.answerDate = "завтра";
                    else $temp.answerDate = $session.date.day + " " + months[$session.date.month].name + " " + $session.date.year;
    #выдаем полный ответ про погоду
            a: Погода в {{capitalize($nlp.inflect($session.place.name, "datv"))}} на {{$temp.answerDate}}: {{($temp.weather.descript).toLowerCase()}}, {{$temp.weather.temp}} {{$nlp.conform("градус", $temp.weather.abs_temp)}}. Ветер {{$temp.weather.wind}}, с порывами {{$temp.weather.gust}} {{$nlp.conform("метр", $temp.weather.gust)}} в секунду
    #если ответ не пришел - извиняемся и идем в главное меню
        else:
            random:
                a: Запрос погоды не получен по техническим причинам. Пожалуйста, попробуйте позже
                a: Что-то поломалось, пока мы узнавали погоду для Вас, давайте попробуем позже
            go!: /Menu/Begin
    #идем спрашивать клиента про климат
        go!: /Weather/Step5
    
    #функция запроса исторических данных о погоде
    state: HistoryStep4
        script:
    #формируем для запроса входную и выходную дату в прошлом году
            $temp.historyDay1 = minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.date.month + "-" + $session.date.day;
            $temp.historyDay2 = minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.date.month + "-" + plus($session.date.day);
    #запрашиваем погоду  по API и сохраняем температуру
            $temp.weather = getHistoricalWeather($session.coordinates.lat, $session.coordinates.lon, $temp.historyDay1, $temp.historyDay2);
            $session.temperature = $temp.weather.temp;
    #если ответ пришел - выдаем его
        if: $temp.weather
    #формируем часть ответа про дату, на который получен прогноз
            script: $temp.answerDate = $session.date.day + " " + months[$session.date.month].name; 
    #выдаем полный ответ про погоду
            a: Погода в {{capitalize($nlp.inflect($session.place.name, "loct"))}} в прошлом году на {{$temp.answerDate}} была: {{$temp.weather.temp}} {{$nlp.conform("градус", $temp.weather.abs_temp)}}. Ветер {{$temp.weather.wind}}, с порывами {{$temp.weather.gust}} {{$nlp.conform("метр", $temp.weather.gust)}} в секунду.
    #если ответ не пришел - извиняемся и идем в главное меню
        else:
            random:
                a: Запрос погоды не получен по техническим причинам. Пожалуйста, попробуйте позже
                a: Что-то поломалось, пока мы узнавали погоду для Вас, давайте попробуем позже
            go!: /Menu/Begin
    #идем спрашивать клиента про климат
        go!: /Weather/Step5

    state: Step5
    #уточняем: точно ли клиент хочет поехать в умеренный/холодный/теплый климат
        if: ($session.temperature < 25 && $session.temperature > 0)
            random:
                a: Вы действительно планируете поездку в страну с умеренным климатом?
                
        if: ($session.temperature < 0 || $session.temperature == 0)
            random:
                a: Вы действительно планируете поездку в страну с холодным климатом?

        if: ($session.temperature > 25 || $session.temperature == 25)
            random:
                a: Вы действительно планируете поездку в страну с жарким климатом?
        buttons:
            "Да, планирую"
            "Нет, не планирую"
    #введен город/страна - запомнили их и идем на старт погоды
        state: Location
            q: * $City *
            q: * $Country *
            script: getLocation ($parseTree)
            a: {{$session.place.name}}? Сейчас узнаю какая там погода.
            go!: /Weather/Begin
    #введена дата - запомнили её и идем на Шаг3 погоды
        state: Date
            q: * @duckling.date *
            script: $session.date = $parseTree.value;
            go!: /Weather/Step3
    #ответ нет - идем на шаг6 погоды
        state: NoSure
            q: $no
            q: * не планир* *
            go!: /Weather/Step6
    #если да - предлагаем оформить заявку (или продолжить её оформление)
        state: YesSure
            q: $yes
            q: планирую *
            if: $session.tripStep
                random:
                    a: Продолжим оформление заявки на тур?
                    a: Продолжаем заполнять заявку на тур?
                buttons:
                    "Да, продолжим"
                    "Нет, не надо"    
            else: 
                random:
                    a: Давайте оформим заявку на тур в {{capitalize($nlp.inflect($session.place.name, "accs"))}}?
                    a: Оформляем заявку на тур в {{capitalize($nlp.inflect($session.place.name, "accs"))}}?
                buttons:
                    "Да, оформим"
                    "Нет, не надо"                

    #если да - идем в раздел Заявка
            state: Yes
                q: $yes
                q: $tour
                q: * продолж*
                go!: /Trip/Begin
    #если нет - идем на шаг6 погоды    
            state: Deny
                q: $no
                q: ( * (не надо)/(не хочу)/(не буду) * )
                q: $refuse
                go!: /Weather/Step6        
                    
    state: Step6                
        a: Давайте посмотрим климат в другом месте?
        buttons: 
            "Прогноз в другом месте"
            "Не нужен прогноз"
    #другое место очищаем место и дату, идем на начало прогноза
        state: ChangePlaceDate
            q: * ~другой [~место] *
            q: * ~другой ~дата *
            q: $yes
            script:
                delete $session.place, 
                delete $session.coordinates, 
                delete $session.date
            go!: /Weather/Begin     
    #не нужен прогноз - идем на выход
        state: Deny
            q: $no
            q: ( * (не надо)/(не хочу)/(не буду) * )
            q: $refuse
            a: Как скажете!
            go!: /Exit
            
