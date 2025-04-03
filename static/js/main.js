// API Test functionality
document.addEventListener('DOMContentLoaded', function() {
    const apiTestForm = document.getElementById('api-test-form');
    const responseElement = document.getElementById('response');
    
    if (apiTestForm) {
        apiTestForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const endpoint = document.getElementById('endpoint').value;
            
            // Display loading message
            responseElement.textContent = 'Loading...';
            
            // Send API request
            fetch(endpoint)
                .then(response => response.json())
                .then(data => {
                    // Format and display the response
                    responseElement.textContent = JSON.stringify(data, null, 2);
                })
                .catch(error => {
                    responseElement.textContent = 'Error: ' + error.message;
                });
        });
    }
});
