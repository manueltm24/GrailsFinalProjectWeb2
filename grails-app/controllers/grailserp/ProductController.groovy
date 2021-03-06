package grailserp

import grails.converters.JSON
import org.springframework.security.access.annotation.Secured
import org.springframework.validation.BindingResult
import org.springframework.web.multipart.MultipartFile

import static org.springframework.http.HttpStatus.*

class ProductController {

    def index = {
        List<Product> products = Product.findAllByQuantityGreaterThan(0)
        [productList: products?:[]]
    }

    def render_image = {
        def product = Product.get(params.id)

        response.contentType = product.imageType
        response.contentLength = product.productImage.size()
        OutputStream out = response.outputStream
        out.write(product.productImage)
        out.close()
    }

    @Secured(['ROLE_ADMIN','ROLE_SUPPLY'])
    def create() { }

    @Secured(['ROLE_ADMIN','ROLE_SUPPLY'])
    def save() {
        Product product = new Product(params)

        if (product == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (product.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond product.errors, view:'create'
            return
        }

        MultipartFile imageFile = request.getFile('productImage')
        product.productImage = imageFile.bytes
        product.imageType = imageFile.contentType

        product.save(failOnError: true, flush: true)

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'product.label', default: 'Product'), product])
                redirect product
            }
            '*' { respond product, [status: CREATED] }
        }
    }

    @Secured(['ROLE_ADMIN','ROLE_SUPPLY'])
    def edit() {
        Product product = Product.get(params.id)
        [product: product]
    }

    @Secured(['ROLE_ADMIN','ROLE_SUPPLY'])
    def update() {
        Product product = Product.get(params.id)

        if (product == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (product.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond product.errors, view:'edit'
            return
        }

        product.properties = params as BindingResult
        product.save(failOnError: true, flush: true)

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'product.label', default: 'Product'), product])
                redirect product
            }
            '*'{ respond product, [status: OK] }
        }
    }

    @Secured(['ROLE_ADMIN','ROLE_SUPPLY','ROLE_SALES'])
    def show() {
        Product product = Product.get(params.id)
        [product: product]
    }

    @Secured(['ROLE_ADMIN','ROLE_SUPPLY','ROLE_SALES'])
    def list() {
        def products = Product.findAllByQuantityGreaterThan(0)
        def data = ['aaData': products]
        render data as JSON
    }

    def show_user() {
        Product product = Product.get(params.id)
        def otherProducts = Product.find("from Product order by rand()", [max:4])
        render view: "show_user", model:[others: otherProducts, product: product]
    }

    def delete() {
        Product product = Product.get(params.id)

        if (product == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        product.delete(failOnError: true, flush: true)

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'product.label', default: 'Product'), product])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'product.label', default: 'Product'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
    
}
