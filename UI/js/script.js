var serviceURL = 'http://localhost:25810/'
var xhr;
var cache = {};

if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(showPosition);
} 
var latitude;
var longitude;
function showPosition(position) {
    latitude = position.coords.latitude;
    longitude = position.coords.longitude;
}

function FetchSuggestions(query,keyCode) {

    if (xhr != null) xhr.abort();
    var url = serviceURL+'wordmatch?query='+query+'&format=text';
    if (keyCode == 32) {
        url = serviceURL+'autosuggest?query='+query.trim()+'&format=text';
    }
    xhr = $.ajax({
        url: url,
        success: function (data) {
            obj = JSON.parse(data);
            if(obj.length > 0){
                var resultHTML = '<div id="search-results"><ol class="search_list">';
                for(var job in obj){
                    resultHTML += '<li class="result-item"><a class="search_link">';
                    resultHTML += '<h4 class="search_title">'+obj[job]+'</h4></a></li>';   
                }
                resultHTML += '</ol></div>';
                $('.search-form_liveout').html(resultHTML);
            }
            xhr = null;
        },
        complete: function () {
            
        },
        error: function (errorThrown) {
            
        }
    });
    
}


$(document).on('click','.jobUrl', function(e){
    e.preventDefault();
    var _query = $('.search-query strong').html().trim();
    var _loggedUrl = $(this).attr('data-href');
    var logText = encodeURIComponent(_query + '\t' + _loggedUrl + "\n");
    $.ajax({
        url: serviceURL+'log?query='+logText,
        cache: false,
        success: function(msg) {
            var win = window.open(_loggedUrl, '_blank');
        },
        error: function() {
            // Fail message
        },
    });
});

$('.dropdown-menu--horizontal a').on('click', function(e){
    e.preventDefault();
    $('.dropdown-menu--horizontal a').removeClass('active');
    $(this).addClass('active');

});
$(document).on('click','.search-form_submit',function(e){

   /* $('header').slideUp("slow",function() {
        //$( "#msg" ).text( $( "button", this ).text() + " has completed." );
    });
    $('header').animate({
        top: '-50%'
    }, 500, function() {
        $('header').css('top', '-150%');
        $('header').css('position', 'absolute');
        //$(this).appendTo('#container');
    });
    */
	if (xhr != null) xhr.abort();
    var query = $('.search-form_input').val();
    if(!query.trim()){
        return;
    }
    $('.search-query strong').html('');
    $('#search-results-section').addClass('hide');
	// move to success
    $(".results-container").html('');
    var sort = $('.dropdown-menu--horizontal a.active').attr('data-type');
    var url = serviceURL+'search?query='+encodeURIComponent(query)+'&ranker=favorite&format=json&lat=' + latitude + '&longitude='+ longitude + '&sort=' + sort;
    $.ajax({
        url: url,
        success: function (data) {
        	obj = JSON.parse(data);
            if(obj.data.length > 0){
                
            	$('.search-query strong').html(obj.correctQuery);
                $('.search-query i').html(obj.location);
            	$('html, body').animate({
            		scrollTop: $(".scrollTo").offset().top - 50
    		    }, 2000);
    		    $('#search-results-section').removeClass('hide');
    		    $('.search-form_input').val('')
                // Render the books using the template
                $("#searchResultTemplate").tmpl(obj.data).appendTo(".results-container");
                
                $( ".rating" ).each(function() {
                    var rating = $(this).attr('data-value');
                    $(this).rateYo({
                        rating    : rating,
                        spacing   : "4px",
                        halfStar: true,
                        readOnly: true,
                        starWidth: "15px"
                    });
                });
                
            }
            else{
                $('#search-results-section').addClass('hide');
                alert('No results found!');
            }
		    
        },
        complete: function () {
            $('.spinner').addClass('hide');
        },
        beforeSend: function(){
            $('.spinner').removeClass('hide');
        },
        error: function (errorThrown) {
            
        }
    });
});

$(document).on('click','#search-results .result-item .search_link', function(e){
    var query = $(this).find('.search_title').html();
    $('.search-form_input').val(query);
    $('#search-form_liveout').html('');
    $('.search-form_input').blur();
});

$('.search-form_input').focusout(function() {
    $('#search-form_liveout').html('');
});

$(document).on('keyup', '.search-form_input', function (e) {
    var inputVal = $(this).val();
    offset = 0;
    $('.search-form_liveout').html('');
    if (inputVal.length < 3) {
        return false;
    }
    FetchSuggestions(inputVal, e.keyCode);
});

$(".search-form_input").on('keyup', function (e) {
    if (e.keyCode == 13) {
        $('.search-form_liveout').html('');
        //console.log('Enter Detected');
        $('.search-form_submit').click();
    }
});