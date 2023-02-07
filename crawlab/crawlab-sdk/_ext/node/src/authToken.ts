import {UnaryInterceptor, Request, UnaryResponse} from 'grpc-web';

class AuthTokenInterceptor implements UnaryInterceptor<typeof Request, UnaryResponse<typeof Request, any>> {
  intercept(request: Request<any, any>, invoker: (request: Request<typeof Request, UnaryResponse<typeof Request, any>>) => Promise<UnaryResponse<typeof Request, UnaryResponse<typeof Request, any>>>): Promise<UnaryResponse<typeof Request, UnaryResponse<typeof Request, any>>> {
    // before RPC request
    const headerName = 'authorization';
    const metadata = request.getMetadata();
    metadata[headerName] = _getAuthTokenEnv();
    request.getMethodDescriptor().createRequest(request, metadata);

    // after the RPC returns successfully.
    return invoker(request);
  }
}


const _getAuthTokenEnv = (): string => {
  return process.env.CRAWLAB_GRPC_AUTH_KEY || '';
};

let authTokenInterceptor: AuthTokenInterceptor;

export const getAuthTokenInterceptor = (): any => {
  if (!authTokenInterceptor) {
    authTokenInterceptor = new AuthTokenInterceptor();
  }
  return new AuthTokenInterceptor();
};

export const getAuthToken = (): string => {
  return _getAuthTokenEnv();
};
