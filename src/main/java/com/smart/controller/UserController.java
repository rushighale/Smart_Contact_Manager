package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
		
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	@Autowired
	private MyOrderRepository myOrderRepository;
	
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model m,Principal principal) {
		 String userName=principal.getName();
		 System.out.println("USERNAME"+userName);	 
		 User user=userRepository.getUserByUserName(userName);	 
		 System.out.println("USER"+user);
		m.addAttribute("user",user);
		 // get the user using  userName
	}
	
	
	// dashboard home 
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal){		 	 
		return "normal/user_dashboard";
	}
	
	
	// Open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
		
	}
	
	
	
	// processing add contact form
	@PostMapping("/process-contact")       									 // we keep form ka data contact entity ka hai(same fileds fo class and form)
	public String processContact(@ModelAttribute Contact contact,
								@RequestParam("profileImage") MultipartFile file ,
								Principal principal,HttpSession session) {	
		try {
		String name=principal.getName();	
		User user=this.userRepository.getUserByUserName(name);
		
		// processing and uploading file
		
		if(file.isEmpty()) {
			
			//if the file is empty then try our message
			System.out.println("file is empty");
			contact.setImage("contact_logo.png");
			
		}else {
			
			//file the file to folder and update name to contact
			contact.setImage(file.getOriginalFilename());
			
			File saveFile=new ClassPathResource("static/img").getFile();
			Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			
			
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);   // agar file exist hai folder mai to use replace kro,souce se sari byte ko collect krega and folder me put krega
			System.out.println("Image is uploaded");
		
		}
		
		user.getContacts().add(contact);									    // we can make seprate contact repository for this
		contact.setUser(user);   						 			    	  // contact ko user dena hai and user mai contact beacuse bydirectional mapping
		
		
		this.userRepository.save(user);                  					  // updat the user,
				
		System.out.println("Data"+ contact);
		System.out.println("Added to data base");
		
		
		// message success
		session.setAttribute("message", new Message("Your Contact is added !! add More..","success"));
		
	}catch(Exception e) {
		
			System.out.println("ERROR"+e.getMessage());
			e.printStackTrace();
			// error message
			
			session.setAttribute("message", new Message("something went wrong !! try again..","danger"));
	
	}
		return "normal/add_contact_form";	
	}
	
	
	// show contact handler
	//per page = 5[n]
	//current page = 0 [page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m,Principal principal) {
		
		m.addAttribute("title", "show user contacts" );
		
		// contact ki list bhejni hai
		String userName=principal.getName();                                // principl>userName>user>id>contacts
		User user=this.userRepository.getUserByUserName(userName);
		
		Pageable pageable=PageRequest.of(page, 5);							// parent ke variable mai child ko store kr skte hai(Pageable is parent interface)
		
		Page<Contact> contacts=this.contactRepository.findContactsByUser(user.getId(),pageable);	
		
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	} 
	
	
	// showing particular contact details.
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model model,Principal principal) {
		
	System.out.println("CID"+cId);
		
	Optional<Contact> contactOptional=this.contactRepository.findById(cId);
	Contact contact=contactOptional.get();
		
	//
	String userName= principal.getName();
	User user=this.userRepository.getUserByUserName(userName);
	
	if(user.getId()==contact.getUser().getId()) {
		model.addAttribute("contact",contact);
	}
	
	
	
		return "normal/contact_detail";
	}
	
	//Delete contact Handler
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable ("cid") Integer cId, Model model,Principal principal,HttpSession session) {
		
		Contact contact =this.contactRepository.findById(cId).get();
//		Optional<Contact>  contactOptional=this.contactRepository.findById(cId);
//		Contact contact=contactOptional.get(); 
		
		
		//check..
		System.out.println("contact "+contact.getcId());
		//unlink the contact from user (cascading problem while deleting contact)
		
		User user=this.userRepository.getUserByUserName(principal.getName()); 
		
		user.getContacts().remove(contact);          // jo list milegi usse contact remove krenge
		
		this.userRepository.save(user);				// if this user is allready is availabe then update it
		// remove
		//img
		// contact.getImage(), combine path and remove photo
		
		
		
		
		
		
		this.contactRepository.delete(contact);                      // or you can use deleteById but we want to check so we choose anther method
		session.setAttribute("message", new Message("contact deleted successfully", "success"));
		
		
		return "redirect:/user/show-contacts/0";
	}
	
	//open update form handler
	@PostMapping("/update-contact/{cid}")														// if you used post , then allready checked beacuse there is not work for url, work only for button					
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {
		
		m.addAttribute("title","update Contact");
		Contact contact=this.contactRepository.findById(cid).get();
		
		m.addAttribute("contact",contact);
		return "normal/update_form";
	}
	
	// update contact handler
	@RequestMapping(value="/process-update",method= RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal,HttpSession session ) {
		
		try {
			
			//old contactDetails
			Contact oldcontactDetail=this.contactRepository.getById(contact.getcId());
			
			
			
			// image
			if(!file.isEmpty()) {
				// file work...
				// rewrite
				
				// delete old photo
				File deleteFile=new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile, oldcontactDetail.getImage());
				
				file1.delete();
				
				// update new photo
				File saveFile=new ClassPathResource("static/img").getFile();
				Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);   // agar file exist hai folder mai to use replace kro,souce se sari byte ko collect krega and folder me put krega
				contact.setImage(file.getOriginalFilename());
				
			}else {
				contact.setImage(oldcontactDetail.getImage());
			}
			
			User user=this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			
			session.setAttribute("message", new Message("You contact is updated.....!","success"));
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("contact name"+contact.getName());
		System.out.println("contact name"+contact.getcId());
		
		return "redirect:/user/"+contact.getcId()+"/contact";         // show contact detais with updated
	}
	
	
	//your profile handler
	@GetMapping("/profile")
	public String YourProfile(Model model) {
		
		
		
		model.addAttribute("title","Profile Page");
		
		
		
		return "normal/profile";
	}
	
	
	
	 // Open setting  handler
	@GetMapping("/settings")
	public String openSettings() {
		
		return "normal/settings";
	}
	
	//change password.... handler
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword")  String oldPassword,
			@RequestParam("newPassword") String newPassword,
			Principal principal,HttpSession session) {
		
		String userName=principal.getName();
		User currentUser=this.userRepository.getUserByUserName(userName);
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			
			// change password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your Password is successfully changed.....!","success"));
		}else {
			
			session.setAttribute("message", new Message("wrong old password.....!","danger"));
			return "redirect:/user/settings";
		}
		
		
		
		System.out.println("oldpassword"+oldPassword);
		System.out.println("NEWPASSWORD"+newPassword);
		return "redirect:/user/index";
	}
	
	
	
	
	
	//creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String,Object> data,Principal principal) throws Exception {
		System.out.println(data);
		int amt=Integer.parseInt(data.get("amount").toString());   // data->string->int
		
	 var client	=new RazorpayClient("rzp_test_mmLdgSvHJDnpaF","9pHJk6dXF2wZ8uJteQGq4N0q");
		
	 JSONObject options = new JSONObject();
	 options.put("amount", amt*100);
	 options.put("currency", "INR");
	 options.put("receipt", "txn_123456");
	 
	 // creating new order  
	 
	 Order order = client.Orders.create(options);   // requst->razorpayserver(create order)->we ger order in return    // pass details to options
	 
	 System.out.println(order);
	 // if you want uou can save this to your data
	 
	 MyOrder myOrder=new MyOrder();
	 myOrder.setAmount(order.get("amount")+"");
	 myOrder.setOrderId(order.get("id"));
	 myOrder.setPaymentId(null);
	 myOrder.setStatus("created");
	 myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
	 myOrder.setRecipt(order.get("receipt"));
	 
	 this.myOrderRepository.save(myOrder);
	 
	 return order.toString(); 
		
	 
	}
	
	// 
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String,Object> data){
		System.out.println(data);
		
		MyOrder myOrder=this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(myOrder);
		return ResponseEntity.ok(Map.of("msg","updated"));
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}


