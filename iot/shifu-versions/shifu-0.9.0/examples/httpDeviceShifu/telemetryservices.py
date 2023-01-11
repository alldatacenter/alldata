from flask import Flask, request, make_response

app = Flask(__name__)

@app.route('/endpoint1', methods=['POST'])
def endpoint1():
    print("from endpoint1:")
    print(request.data)
    response = make_response("status api", 200)
    response.mimetype = "text/plain"
    return response

@app.route('/api1', methods=['POST'])
def api1():
    print("from api1:")
    print(request.data)
    response = make_response("status api", 200)
    response.mimetype = "text/plain"
    return response

@app.route('/status')
def status():
    response = make_response("status api", 200)
    response.mimetype = "text/plain"
    return response

@app.route('/info')
def info():
    response = make_response("info api", 200)
    response.mimetype = "text/plain"
    return response

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=12345)
