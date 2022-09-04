import uvicorn as uvicorn
from fastapi import FastAPI

app = FastAPI() # 必须实例化该类，启动的时候调用


@app.get('/')
async def index():
    return {'message': '欢迎来到FastApi 服务！'}

if __name__ == '__main__':
    uvicorn.run(app=app, host="127.0.0.1", port=8080)
