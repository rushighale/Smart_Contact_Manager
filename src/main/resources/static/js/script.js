const toggleSidebar = () => {
	if ($(".sidebar").is(":visible")) {
		// true 
		// band krna hai
		$(".sidebar").css("display", "none");
		$(".content").css("margin-left", "0%");
	} else {

		// false
		// show krna hai
		$(".sidebar").css("display", "block");
		$(".content").css("margin-left", "20%");
	}
}

const search = () => {
	// console.log("searching..");
	let query = $("#search-input").val();
	console.log(query);


	if (query == '') {
		$(".search-result").hide();
	} else {
		// search
		console.log(query);

		// sending request to server
		let url = `http://localhost:8080/search/${query}`;
		fetch(url)
			.then((response) => {
				return response.json();
			})
			.then((data) => {
				// data....      return response.json(); you get this is in data

				console.log(data);                // process the data into html   

				let text = `<div class='list-group'>`

				data.forEach((contact) => {
					text += `<a  href='/user/${contact.cId}/contact' class='list-group-item list-group-item-action'>${contact.name} </a>`

				});


				text += `</div>`;

				$(".search-result").html(text);
				$(".search-result").show();
			});

		$(".search-result").show();

	}
};
//------------PAYEMENT INTEGRATION---------------------------
// first request to server to craete order

const paymentStart = () => {
	console.log("payement started")
	let amount = $("#payment_field").val();       //id=payment field
	console.log(amount)
	if (amount == '' || amount == null) {
		//alert("amount is required !!")
		swal("Failed !!", "amount is required !!", "error");
		return;
	}


	//code ....
	// 1.we will use ajax to send request to server

	$.ajax(
		{
			url: '/user/create_order',            // send request to server 
			data: JSON.stringify({ amount: amount, info: 'order_request' }),
			contentType: 'application/json',
			type: 'POST',
			dataType: 'json',
			success: function(response) {

				//invoked where seccess
				console.log(response)
				if (response.status == "created") {
					//open payment form
					let options = {
						key: 'rzp_test_mmLdgSvHJDnpaF',    // key 
						amount: response.amount,
						currency: 'INR',
						name: 'Smart Contact Manager',
						description: 'Donation',
						image: '',
						order_id: response.id,
						handler: function(response) {
							console.log(response.razorpay_payment_id)
							console.log(response.razorpay_order_id)
							console.log(response.razorpay_signature)
							console.log('payment successfull !!')
						//	alert("congrats !! payment suceesfull")
						
						updatePaymentOnServer(response.razorpay_payment_id,response.razorpay_order_id,'paid');
						
						
							

						},
						"prefill": {                            // prefill means data pahile se hi likhkar ayega
							"name": "",
							"email": "",
							"contact": ""
						},
						"notes": {
							"address": "Learn code with Durgesh"

						},
						"theme": {
							"color": "#3399cc"
						},

					};


					// for initiate payment
					let rzp = new Razorpay(options);

					// alert when raorpay payment failed
					rzp.on("payment.failed", function(response) {
						Console.log(response.error.code);
						Console.log(response.error.description);
						Console.log(response.error.source);
						Console.log(response.error.step);
						Console.log(response.error.reason);
						Console.log(response.error.metadata.order_id);
						Console.log(response.error.metadata.payment_id);
					//	alert("Oopes payments failed")
						swal("Failed !!", "Oopes payments failed", "error");


					});

					// form opens automatically
					rzp.open();


				}


			},
			error: function(error) {

				// invoked when error
				console.log(error)
				alert("something went wrong!!")

			}
		}
	)



}


// 
    function updatePaymentOnServer(payement_id,order_id, status)
{
	$.ajax(
		{
			url: '/user/update_order',            // send request to server 
			data: JSON.stringify({payement_id: payement_id, order_id: order_id,status:status }),
			contentType: 'application/json',
			type: 'POST',
			dataType: 'json',
			success:function(response){
				swal("Good job!", "congrats !! payment suceesfull", "success");
			},
			error:function(error){
				swal("Failed !!", "Your payment is successfull,but we did not get on server, we will contact as soon as possible", "error");
			}
})

}




