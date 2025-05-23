/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bookstore.csa.bookstore.resource;

import com.bookstore.csa.bookstore.db.InMemoryDatabase;
import com.bookstore.csa.bookstore.exception.BookNotFoundException;
import com.bookstore.csa.bookstore.exception.CartNotFoundException;
import com.bookstore.csa.bookstore.exception.CustomerNotFoundException;
import com.bookstore.csa.bookstore.exception.OutOfStockException;
import com.bookstore.csa.bookstore.model.Book;
import com.bookstore.csa.bookstore.model.Cart;
import com.bookstore.csa.bookstore.model.CartItem;
import java.util.logging.Level;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;

/**
 * resource class for managing shopping carts of customers
 * provides endpoints for CRUD operations
 * @author Hp
 */
@Path("/customers/{customerId}/cart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartResource {
    
    private static final Logger LOGGER = Logger.getLogger(CartResource.class.getName());
    private InMemoryDatabase database = InMemoryDatabase.getInstance();
    
    /**
     * retrieves the cart for a given customer
     * creates a new cart if does not exist
     * @param customerId customer ID
     * @return 
     */
    @GET
    public Cart getCart(@PathParam("customerId") Long customerId){
        
        LOGGER.log(Level.INFO, "Fetching cart for customer ID: {0}", customerId);

        // check if customer exists
        if(!database.customerExists(customerId)){
            LOGGER.log(Level.WARNING, "Cart not found for customer ID: {0}", customerId);
            throw new CartNotFoundException("Cart for customer with ID " + customerId + " does not exist");
        }
        
        // get or create cart
        Cart cart = database.getCartByCustomerId(customerId);
        if(cart == null){
            cart = database.createCart(customerId);
            LOGGER.log(Level.INFO, "New cart created for customer ID: {0}", customerId);
        }
        
        return cart;
    }
    
    /**
     * add a book item to the customer's cart
     * @param customerId
     * @param item cart item to be added
     * @return update cart object
     */
    @POST 
    @Path("/items")
    public Cart addToCart(@PathParam("customerId") Long customerId, CartItem item){
        LOGGER.log(Level.INFO, "Adding item to cart for customer ID: {0}", customerId);
        // check if customer exists
        if(!database.customerExists(customerId)){
            LOGGER.log(Level.WARNING, "Customer not found: {0}", customerId);
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exists");
        }
        
        // check if book exists
        Book book = database.getBookId(item.getBookId());
        if(book == null){
            LOGGER.log(Level.WARNING, "Book not found: {0}", item.getBookId());
            throw new BookNotFoundException("Book with ID " + item.getBookId() + " does not exists");
        }
        
        // check quantity
        if(item.getQuantity() <=0 ){
            LOGGER.log(Level.WARNING, "Invalid quantity for book ID: {0}", item.getBookId());
            throw new OutOfStockException("Quantity must be greater than zero");
        }
        
        //Add to cart
        Cart updatedCart = database.addToCart(customerId,item);
        if(updatedCart == null){
            LOGGER.log(Level.WARNING, "Not enough stock for book ID: {0}", item.getBookId());
            throw new OutOfStockException("Not enough stock available for book with ID " + item.getBookId() + " available books: " + book.getStock() + " ,requested quantity: " + item.getQuantity());
        }
        
        return updatedCart;
    }
    
    /**
     * updates the quantity of a book item the customer's cart
     * @param customerId CustomerId Customer ID
     * @param bookId BOOK ID
     * @param item
     * @return updated cart object
     */
    @PUT
    @Path("/items/{bookId}")
    public Cart updateCartItem(
                    @PathParam("customerId") Long customerId,
                    @PathParam("bookId")Long bookId,
                    CartItem item){
        
        // Use the quantity from the CartItem object
        int quantity = item.getQuantity();
        
        // check if customer exists
        if(!database.customerExists(customerId)){
            LOGGER.log(Level.WARNING, "Customer not found: {0}", customerId);
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist");
        }
        
        // check if cart exists
        Cart cart = database.getCartByCustomerId(customerId);
        if(cart == null){
            LOGGER.log(Level.WARNING, "Cart not found for customer ID: {0}", customerId);
            throw new CartNotFoundException("Cart for customer with ID " + customerId + " does not exist");
        }
        
        // check if book exists
        Book book = database.getBookId(bookId);
        if(book == null){
            LOGGER.log(Level.WARNING, "Book not found: {0}", bookId);
            throw new BookNotFoundException("Book with ID " + bookId + " does not exist");
        }
        
        // check if book is in cart
        if(!cart.getItems().containsKey(bookId)){
            LOGGER.log(Level.WARNING, "Book not found: {0}", bookId);
            throw new BookNotFoundException("Book with ID " + bookId + " is not in the cart");
        }
        
        // update quantity
        if(quantity <= 0){
            // remove item if quantity is 0 or negative
            Cart updatedCart = database.removeCartItem(customerId, bookId);
            if(updatedCart == null){
                throw new OutOfStockException("Not enough stock available for book with ID " + bookId);
            }
            return updatedCart;
        } else{
            // update quantity
            Cart updatedCart = database.updateCartItem(customerId, bookId, quantity);
            if(updatedCart == null){
                throw new OutOfStockException("Not enough stock available for book with ID " + bookId);
            }
            return updatedCart;
        }
    }
        
    /**
     * removes book item from the customer's cart
     * @param customerId customer ID
     * @param bookId Book ID
     * @return HTTP 204 No cent Response
     */
        @DELETE
        @Path("/items/{bookId}")
        public Response removeCartItem(
                                @PathParam("customerId") Long customerId,
                                @PathParam("bookId") Long bookId){
            
            LOGGER.log(Level.INFO, "Removing item from cart for customer ID: {0}, book ID: {1}", new Object[]{customerId, bookId});
            
            // check if cart exists
            if(!database.customerExists(customerId)){
                throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist");
            }
            
            // remove item
            database.removeCartItem(customerId,bookId);
            
            LOGGER.info("Item removed from cart successfully.");
            return Response.status(Response.Status.NO_CONTENT).build();
                        
        }
}

