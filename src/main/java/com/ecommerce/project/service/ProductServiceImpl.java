package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repo.CartRepo;
import com.ecommerce.project.repo.CategoryRepo;
import com.ecommerce.project.repo.ProductRepo;
import com.ecommerce.project.util.AuthUtil;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductServiceImpl implements ProductService{
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private AuthUtil authUtil;
;

    @Value("${project.image}")
    String filePath;

    @Value("${image.base.url}")
    String imageBaseUrl;

    public ProductResponse getAllProducts(int pageSize, int pageNumber, String sortBy, String sortOrder, String keyword, String category){
        logger.info("Fetching all products with pageSize={}, pageNumber={}, sortBy={}, sortOrder={}", pageSize, pageNumber, sortBy, sortOrder);
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Specification<Product> spec = Specification.where(null);
        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }

        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category));
        }

        Page<Product> productPage = productRepo.findAll(spec, pageDetails);

        if(productPage.isEmpty()) throw new APIException("No product exist!!");

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream().map(product ->{
            ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
            productDTO.setImage(constructImageUrl(product.getImage()));
            logger.info("Product Image: " + productDTO.getImage());
            return productDTO;
        })
        .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageSize(productPage.getSize());
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    public String constructImageUrl(String imageName){
        logger.info("Base url: " + imageBaseUrl);
        return (imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName );
    }

    public ProductDTO addProduct(ProductDTO productDTO, Long categoryId){
        Category category = categoryRepo.findById(categoryId).
                orElseThrow(() -> new ResourceNotFoundException("category", "categoryId", categoryId));

        List<Product> products = category.getProducts();

        boolean isNotPresent = true;
        for(Product p: products){
            if(p.getProductName().equalsIgnoreCase(productDTO.getProductName())){
                isNotPresent = false;
                break;
            }
        }
        if(!isNotPresent) throw new APIException("Product with the same Product Name already exist!");

        Product product = modelMapper.map(productDTO, Product.class);

        product.setImage("default.png");
        product.setCategory(category);
        product.setUser(authUtil.loggedInUser());
        // product.setDiscount(productDTO.getDiscount());
        double specialPrice = product.getPrice() -
                    ((product.getDiscount() * 0.01) * product.getPrice());
        product.setSpecialPrice(specialPrice);

        Product savedProduct = productRepo.save(product);
        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    public ProductResponse getProductsByCategory(Long categoryId, int pageSize, int pageNumber, String sortBy, String sortOrder) {
        Category category = categoryRepo.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProduct = productRepo.findByCategoryOrderBySpecialPriceAsc(category, pageDetails);

        if(pageProduct.isEmpty()) throw new APIException("No product exist for this category id: " + categoryId);

        List<ProductDTO> productDTO = pageProduct.stream().map((product) ->
                    modelMapper.map(product, ProductDTO.class)
                ).toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTO);
        return productResponse;
    }

    public ProductResponse getProductsByKeyword(String keyword, int pageSize, int pageNumber, String sortBy, String sortOrder){
        List<ProductDTO> productDTO = productRepo.findByProductNameContainingIgnoreCase(keyword).stream().map(product ->
                modelMapper.map(product, ProductDTO.class)
                ).toList();

        if(productDTO.isEmpty()) throw new APIException("No product exist for this Keyword: " + keyword);

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTO);
        return productResponse;
    }

    public ProductDTO updateProduct(Long productId, ProductDTO productDTO){
        Product productFromDb = productRepo.findById(productId).orElseThrow(()->(
             new ResourceNotFoundException("Product", "productId", productId)
        ));
        Product product = modelMapper.map(productDTO, Product.class);

        productFromDb.setProductName(product.getProductName());
        productFromDb.setImage(product.getImage());
        productFromDb.setDescription(product.getDescription());
        productFromDb.setQuantity(product.getQuantity());
        productFromDb.setPrice(product.getPrice());
        productFromDb.setDiscount(product.getDiscount());
        productFromDb.setSpecialPrice(product.getSpecialPrice());

        Product savedProduct = productRepo.save(productFromDb);

        List<Cart> carts = cartRepo.findCartsByProductId(productId);

        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream()
                    .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class)).collect(Collectors.toList());

            cartDTO.setProducts(products);

            return cartDTO;

        }).collect(Collectors.toList());

        cartDTOs.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), productId));

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    public ProductDTO deleteProduct(Long productId){
        Product product = productRepo.findById(productId).orElseThrow(()->
                    new ResourceNotFoundException("Product", "productId", productId)
                );
        productRepo.deleteById(productId);

        return modelMapper.map(product, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        // Get the product from DB
        Product productFromDb = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        // Upload image to server
        // Get the file name of uploaded image
        String path = filePath;
        String fileName = fileService.uploadImage(path, image);

        // Updating the new file name to the product
        productFromDb.setImage(fileName);

        // Save updated product
        Product updatedProduct = productRepo.save(productFromDb);

        // return DTO after mapping product to DTO
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }


}
