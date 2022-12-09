//функция извлекает из ответа название города или страны и их координаты и сохраняет в сессионные переменные
function getLocation (answer){
    if (answer.City) {
        $jsapi.context().session.place = {name: answer._City.name, namesc: "", type: "city"};
        $jsapi.context().session.coordinates = {lat: answer._City.lat, lon: answer._City.lon};
    }
    else {
        $jsapi.context().session.place = {name: answer._Country.name, namesc: answer._Country.namesc, type: ""};
        $jsapi.context().session.coordinates = {lat: answer._Country.lat, lon: answer._Country.lon};
    }
}

//функция запрашивает прогноз погоды и возвращает параметры прогноза либо ответ о неудаче
function getWeather(lat, lon, day) {
	var response = $http.get("https://api.weatherbit.io/v2.0/forecast/daily?lat=${lat}&lon=${lon}&lang=${lang}&key=${key}&days=16", {
            timeout: 10000,
            query:{
                // новый ключ
                key: "c31a720db0464fbb8ff87abd57a16a0b",
                lang: "ru",
                lat: lat,
                lon: lon,
                days: toPrettyString(day)
            }
        });
	if (!response.isOk || !response.data) return false

//характеристики погоды записываются каждая в свои переменные 
	var weather = {};
	weather.temp = Math.round(response.data.data[day].temp);            //Значение температуры, градус по шкале цельсия 
	weather.wind = Math.round(response.data.data[day].wind_spd);        //Скорость ветра, метр в сек.
	weather.gust = Math.round(response.data.data[day].wind_gust_spd);   //Порывы ветра, метр в сек.
	weather.abs_temp = Math.abs(weather.temp);	                        //Абсолютное значение температуры, взятое по модулю, градус по шкале цельсия.  
	weather.descript = response.data.data[day].weather.description;     //Описание погоды: ясно, пасмурно, облачно, осадки, туман и т.д.
	if (weather.wind == weather.gust) weather.gust ++                   //Из-за округления значений, может сложиться ситуация, когда значения скорости ветра
                                                                        //и его порывов могут совпасть. Для более комфортного восприятия на слух в этом случае 
	return weather;                                                     //скорость порывов увеличивается на 1 м/с.
}

//функция запрашивает исторические данны о погоде и возвращает параметры прогноза либо ответ о неудаче
function getHistoricalWeather(lat, lon, start_date, end_date) {
	var response = $http.get("https://api.weatherbit.io/v2.0/history/daily?lat=${lat}&lon=${lon}&start_date=${start_date}&end_date=${end_date}&key=${key}", {
            timeout: 10000,
            query:{
                key: "c31a720db0464fbb8ff87abd57a16a0b",
                lat: lat,
                lon: lon,
                start_date: toPrettyString(start_date),
                end_date:  toPrettyString(end_date)
            }
        });
	if (!response.isOk || !response.data) return false
        
	var weather = {};
	weather.temp = Math.round(response.data.data[0].temp);          
	weather.wind = Math.round(response.data.data[0].wind_spd);
	weather.gust = Math.round(response.data.data[0].wind_gust_spd);
	weather.abs_temp = Math.abs(weather.temp)
	if (weather.wind == weather.gust) weather.gust ++
	
	return weather;
}

//функция отнимает один из числа в формате строки
function minus(num) {
	var number = parseInt(num);
	return (number - 1);
}	

//функция добавляет один к числу в формате строки
function plus(num) {
	var number = parseInt(num);
	return (number + 1);
}	

//функция формирует заявку из сессионных переменных и отправляет её на почту
function email () {
    var subject = "Заявка от клиента " + $jsapi.context().client.name;
    var form = "";
    form += " Имя: " + $jsapi.context().client.name + "<br>";
    form += " Телефон: " + $jsapi.context().client.phone + "<br>";
    if ($jsapi.context().session.place) form += " Пункт назначения: " + $jsapi.context().session.place.name + "<br>";
        else form += " Пункт назначения: не определен"  + "<br>";
    if ($jsapi.context().session.noDate) form += " Дата начала поездки: " + $jsapi.context().session.noDate + "<br>";
        else form += " Дата начала поездки: " + $jsapi.context().session.date.day + "." + $jsapi.context().session.date.month + "." + $jsapi.context().session.date.year + "<br>";
    form += " Длительность поездки: " + $jsapi.context().session.duration + "<br>";
    form += " Количество людей: " + $jsapi.context().session.people + "<br>";
    if ($jsapi.context().session.children) form += " Количество детей: " + $jsapi.context().session.children + "<br>";
        else form += " Количество детей: не определено"  + "<br>";
    form += " Бюджет на одного взрослого: " + $jsapi.context().session.budget + "<br>";
    form += " Минимальная звездность отеля: " + $jsapi.context().session.stars + "<br>";
    form += " Комментарий для Менеджера: " + $jsapi.context().session.comment;
            
    var answer = {}    
    answer = $mail.send({
        from: "jaicp_mfti@mail.ru",
        to: ["jaicp_mfti@mail.ru"],
        subject: subject,
        content: form,
        smtpHost: "smtp.mail.ru",
        smtpPort: "465",
        user: "jaicp_mfti@mail.ru",
        password: "eaxhXE3gXmc1eKnyYGYg"
    });
    return (answer);
}