#подгрузили файл с именами модулей, библиотек и справочников
require: requirements.sc

#========================================= СТАРТ И ГЛОБАЛЬНЫЕ ИНТЕНТЫ ==================================================
theme: /  

    #старт диалога с интентов start и приветствия
    state: Welcome
        q!: (*start/старт)
        q!: * Мария *
        q!: $greeting
        script: $session = {}      //обнулили переменные сессии
    #проверяем есть ли имя клиента, представляемся соотв.образом и идем в Меню или Имя
        if: $client.name
            a: Здравствуйте, {{$client.name}}. Это Мария, бот Just Tour.
            go!: /Menu/Begin
        else:
            a: Здравствуйте, я Мария, бот туристической компании Just Tour.
            go!: /Name/AskName

    #глобальный ответ на нераспознанные реплики, контекст не меняется
    state: CatchAll || noContext = true 
        event!: noMatch
        random:
            a: Простите, я не поняла. 
            a: Извините, я не понимаю.
        random:
            a: Попробуйте ответить по-другому.
            a: Переформулируйте Ваш вопрос.

    #глобальный ответ на прощание
    state: Bye || noContext = true 
        q: $goodbye
        random:
            a: Всего доброго!
            a: Всего хорошего!

    #глобальный стейт отказа или отмены
    state: Cancel
        q!: $refuse
        q!: $cancel
        random:
            a: Поняла, выхожу из диалога.
            a: Жаль, а мне было приятно пообщаться
        go!: /Exit
    
    #глобальный стейт при выходе из сценария по желанию клиента
    state: Exit
        random:
            a: Буду на связи. Обращайтесь, если понадоблюсь!
            a: Пока. Надеюсь, ещё услышимся!
        script: $session = {}      //обнулили переменные сессии

    #глобальный стейт при завершении сценария после отправки заявки на тур
    state: End
        a: До свидания!
        script: $session = {}      //обнулили переменные сессии


#============================================ ЗАПРОС ИМЕНИ ===============================================================

theme: /Name

    #запрашиваем имя, любые слова считаем именем, поэтому остаемся в этом стейте
    state: AskName || modal = true
        random:
            a: Как я могу к Вам обращаться?
            a: А вас как зовут?
   
    #имя совпало с переменной из списка - сохраняем его и идем в Меню
        state: GetName
            q: * $Name *
            script: $client.name = $parseTree._Name.name;
            random:
                a: {{$client.name}}, приятно познакомиться!

            go!: /Menu/Begin
    #не хочу знакомиться - соглашаемся и идем в меню
        state: NoName
            q: $refuse
            q: ( * (не надо)/(не хочу)/(не буду) * )
            a: Как вам будет удобно. Обойдемся без знакомства.
            go!: /Menu/Begin
    #выход из модального стейта при запросе погоды
        state: GoWeather
            intent: /Погода
            go!: /Weather/Begin
    #выход из модального стейта при желании выйти из диалога
        state: GoExit
            q: $cancel 
            go!: /Exit         
    #другое непонятное слово - уточняем имя это или нет
        state: GetStrangeName
            event: noMatch
            script: $session.tempName = $request.query;
            random:
                a: {{$session.tempName}}! Какое необычное имя. Вы не ошиблись? Я могу вас так называть?
                a: Какое интересное имя. Я правильно поняла, что могу называть вас {{$session.tempName}}?
                a: Какое необычное имя! Давайте проверим: ваше имя {{$session.tempName}}?
            buttons:
                "Да"
                "Нет"
    #если имя - сохраняем его и идем в Меню
            state: Yes
                q: $yes
                script: $client.name = $session.tempName;
                a: {{$client.name}}, приятно познакомиться!
                go!: /Menu/Begin
    #если не имя - соглашаемся не знакомиться и идем в Меню
            state: No
                q: $no
                random:
                    a: Как вам будет удобно. Обойдемся без знакомства.
                    a: Ну, что ж, знакомиться необязательно. Я и так вам помогу
                    a: Тогда продолжим без знакомств
                go!: /Menu/Begin
                
#====================================================== МЕНЮ ===========================================================

theme: /Menu
    state: Begin
        # random:
        a: Я рассказываю о погоде в разных городах мира и могу оформить заявку на подбор тура.
            # a: Я умею предсказывать погоду во многих городах мира, а ещё могу подобрать вам тур
        go!: /Menu/Choose

    #предложили выбор; ответ про погоду поймает входной интент Погоды
    state: Choose
        # random:
        a: Что Вас интересует?
            # a: О чем поговорим сегодня?
        buttons:
            "Рассказать о погоде"
            "Оформить заявку на тур"
        
    #интент Что еще умеешь - идем в начало выбора       
        state: WhatElse
            q: $whatelse
            random:
                a: Я больше ничего не умею. Только рассказывать о погоде и оформлять заявку на тур.
                a: Пока я умею говорить о погоде и подбирать туры
            go!: /Menu/Choose
    #интент Отказ - идем на выход
        state: Deny
            q: $refuse
            q: ( * (не надо)/(не хочу)/(не буду) * )
            q: $no
            random:
                a: Как скажете.
                a: Жаль. А я бы сейчас съездила на Кубу
            go!: /Exit
    #интент Прогноз погоды    
        state: Weather
            intent: /Погода
            go!: /Weather/Begin
    #интент Оформить заявку
        state: Tour
            q: $tour
            q: * (~тур) *
            random:
                a: В какой город или страну хотите поехать?
                a: В какой город или страну желаете отправиться?
            buttons:
                "Еще не решил"
    #назван город или страна
            state: CityOrCountry
                q:  * $City * 
                q:  * $Country * 
    #запоминаем город или страну и их координаты и идем на начало Заявки
                script: getLocation ($parseTree)
                random:
                    a: Записала, {{$session.place.name}}
                    a: Вот бы и мне сейчас туда. Принято, давайте заполним заявку
                go!: /Trip/Begin
    #не решил или нужна консультация - идем на начало Заявки
            state: NoSure
                q: $dontknow
                q: * ~консультация *
                random:
                    a: Не проблема. Заполним заявку, а менеджер поможет Вам выбрать направление
                    a: Это не беда. Менеджер поможет выбрать вам тур, исходя из ваших пожеланий. А мы с вами пока заполним заявку
                go!: /Trip/Begin
    #все остальные ответы
            state: NoMatch
                event: noMatch
                random:
                    a: Я Вас не поняла. Давайте заполним заявку, а направление выберете потом
                    a: Простите, но я не нашла данное направление. Давайте оформим заявку, а направление выберете с менеджером
                go!: /Trip/Begin    
            