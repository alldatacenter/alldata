
import re
from enum import Enum
from typing import List
from feathr.definition.feature import Feature
from feathr.definition.feature_derivations import DerivedFeature

from feathr.definition.transformation import ExpressionTransformation, WindowAggTransformation
from functions import SUPPORTED_FUNCTIONS

class Token:
    def __init__(self, name, value):
        self.name = name
        self.value = value
    def __str__(self):
        res = 'Token({}, {})'.format(
            self.name, repr(self.value))
        return res
    def __repr__(self):
        return self.__str__()
    def is_identifier(self):
        return self.name == Tokenizer.identifier.name
    def is_number(self):
        return self.name == Tokenizer.number.name
    def is_operator(self):
        return hasattr(Operator, self.name) 
    def is_new_line(self):
        return self.name == Tokenizer.new_line.name
    def is_eof(self):
        return self.name == Tokenizer.eof.name

class Operator(Enum):
    # 2 characters
    power = '**'
    less_equal = '<='
    greater_equal = '>='
    not_equal = '!='
    # 1 character
    plus = '+'
    minus = '-'
    multiply = '*'
    divide = '/'
    mod = '%'
    equal = '='
    less_than = '<'
    greater_than = '>'
    left_curly = '{'
    left_paren = '('
    left_square = '['
    right_curly = '}'
    right_paren = ')'
    right_square = ']'
    comma = ','

class Tokenizer(Enum):
    comment = r'#[^\r\n]*'
    space = r'[ \t]+'
    identifier = r'[a-zA-Z_][a-zA-Z_0-9]*'
    number = r'[0-9]+(?:\.[0-9]*)?'
    operator = r'\*\*|[<>!]=|[-+*/%=<>()[\]{},]'
    new_line = r'[\r\n]'
    eof = r'$'
    error = r'(.+?)'
    @classmethod
    def _build_pattern(cls):
        cls.names = [x.name for x in cls]
        cls.regex = '|'.join('({})'.format(x.value) for x in cls)
        cls.pattern = re.compile(cls.regex)
    @classmethod
    def token_iter(cls, text):
        ''' text to token iter. 
        Args:
            text: string for tokenization.
        Returns:
            Iteration object of generated tokens.
        '''
        for match in cls.pattern.finditer(text):
            name = cls.names[match.lastindex-1]
            # skip space and comment
            if (name == cls.space.name 
                or name == cls.comment.name):
                continue
            # raise error
            elif name == cls.error.name:
                print(text[match.start():])
                raise Exception('Invalid Syntax.')
            value = match.group()
            # operator name
            if name == cls.operator.name:
                name = Operator(value).name
            token = Token(name, value)
            yield token
Tokenizer._build_pattern()

class AST:
    def __init__(self, token):
        self.token = token
    def __repr__(self):
        return self.__str__()
    def __str__(self):
        return str(self.token)

class AtomOp(AST):
    def __init__(self, token, is_func=False):
        self.token = token
        self.value = token.value
        self.is_func = is_func
    def __str__(self):
        if self.token.is_operator():
            return self.token.name
        return self.value
class FuncOp(AST):
    def __init__(self, func=None, ops=None):
        self.func = func
        self.ops = ops
    def __str__(self):
        pstr = ', '.join([str(x) for x in self.ops])
        return '{}({})'.format(self.func, pstr)
class VectorOp(AST):
    def __init__(self, ops=None):
        self.ops = ops
    def __str__(self):
        pstr = ', '.join([str(x) for x in self.ops])
        return '[{}]'.format(pstr)
class SetOp(AST):
    def __init__(self, ops=None):
        self.ops = ops
    def __str__(self):
        pstr = ', '.join([str(x) for x in self.ops])
        return '{'+pstr+'}'

class Parser:
    def __init__(self, token_iter):
        '''Parser.
        Args:
            token_iter: token iterator returned by Tokenizer.
        '''
        self.token_iter = token_iter
        self.forward()
    def forward(self):
        '''Set current token by next(token_iter). '''
        self.current_token = next(self.token_iter)
    def error(self, error_code, token):
        '''Trigger by unexpected input. '''
        raise ValueError(
            f'{error_code} -> {token}',
        )
    def parse(self):
        """ generate ast. """
        node = self.expr()

        if not self.current_token.is_eof():
            self.error(
                error_code="UNEXPECTED_TOKEN",
                token=self.current_token,
            )
        return node

    def expr(self):
        """ expr: set_expr|vec_expr|add_expr """
        if self.current_token.name == Operator.left_curly.name:
            node = self.set_expr()
        elif self.current_token.name == Operator.left_square.name:
            node = self.vec_expr()
        else:
            node = self.add_expr()
        return node
    def set_expr(self):
        """ set_expr: { arglist } """
        assert self.current_token.name == Operator.left_curly.name
        self.forward()
        ops = self.arglist()
        assert self.current_token.name == Operator.right_curly.name
        self.forward()
        node = SetOp(ops=ops)
        return node
    def vec_expr(self):
        """ vec_expr: [ arglist ] """
        assert self.current_token.name == Operator.left_square.name
        self.forward() 
        ops = self.arglist()
        assert self.current_token.name == Operator.right_square.name
        self.forward()
        node = VectorOp(ops=ops)
        return node
    def add_expr(self):
        """ add_expr: mul_expr ([+-] mul_expr)* """
        node = self.mul_expr()
        while self.current_token.name in (Operator.plus.name, 
                                          Operator.minus.name):
            op = AtomOp(self.current_token, is_func=True)
            self.forward()
            node = FuncOp(func=op, ops=[node, self.mul_expr()])
        return node
    def mul_expr(self):
        """ mul_expr: factor ([*/%] factor)* """
        node = self.factor()
        while self.current_token.name in (Operator.multiply.name, 
                                          Operator.divide.name, Operator.mod.name):
            op = AtomOp(self.current_token, is_func=True)
            self.forward()
            node = FuncOp(func=op, ops=[node, self.factor()])
        return node
    def factor(self):
        """ factor: [+-] factor | power """
        token = self.current_token
        if (token.name == Operator.minus.name 
            or token.name == Operator.plus.name):
            op = AtomOp(token, is_func=True)
            self.forward()
            node = FuncOp(func=op, ops=[self.factor()])
            return node
        else:
            node = self.power()
            return node
    def power(self):
        """ power: term [** factor] """
        node = self.term()
        if self.current_token.name == Operator.power.name:
            op = AtomOp(self.current_token, is_func=True)
            self.forward()
            node = FuncOp(func=op, ops=[node, self.factor()])
            return node
        return node
    def term(self):
        """ term: function | ( add_expr ) """
        if self.current_token.name == Operator.left_paren.name:
            self.forward()
            node = self.add_expr()
            assert self.current_token.name == Operator.right_paren.name
            self.forward()
        else:
            node = self.function() 
        return node
    def function(self):
        """ function: atom ( arglist? ) """
        assert (self.current_token.is_identifier() 
                or self.current_token.is_number())
        token = self.current_token
        self.forward()
        is_func = (self.current_token.name == Operator.left_paren.name)
        node = AtomOp(token, is_func=is_func)
        if self.current_token.name == Operator.left_paren.name:
            self.forward()
            arglist = []
            if self.current_token.name != Operator.right_paren.name:
                arglist = self.arglist()
            assert self.current_token.name == Operator.right_paren.name
            self.forward()
            node = FuncOp(func=node, ops=arglist)
        return node
    def arglist(self):
        """ arglist: expr (, expr)* """
        res = [self.expr()]
        while self.current_token.name == Operator.comma.name:
            self.forward()
            res.append(self.expr())
        return res

def parse(txt) -> AST:
    """ parse txt to AST. """
    return Parser(Tokenizer.token_iter(txt)).parse()

def get_identifiers(txt) -> list:
    ast = parse(txt)
    s = set()
    return collect_id(ast, s)

def collect_id(ast, s):
    if isinstance(ast, AtomOp):
        # if ast.is_func:
        s.add(ast.token.value)
    elif isinstance(ast, FuncOp):
        if re.match("^[A-Za-z_][A-Za-z0-9_]*$", ast.func.token.value):
            if ast.func.value not in SUPPORTED_FUNCTIONS:
                raise NotImplementedError(ast.func.value)
        for op in ast.ops:
            collect_id(op, s)
    elif isinstance(ast, VectorOp):
        for op in ast.ops:
            collect_id(op, s)
    elif isinstance(ast, SetOp):
        for op in ast.ops:
            collect_id(op, s)
    else:
        raise ValueError(f"unknown ast type: {ast}")
    return s

def gen_dsl(name: str, features: List[Feature]):
    """Generate a dsl file for the given features"""
    
    layers = []
    
    # Add all upstreams to the current_features
    current_features = features.copy()
    while True:
        cf = current_features.copy()
        for f in current_features:
            if isinstance(f, DerivedFeature):
                for uf in f.input_features:
                    if uf not in current_features:
                        cf.append(uf)
        if len(cf) == len(current_features):
            break
        current_features = cf
    
    feature_names = set([f.name for f in current_features])
    
    # Topological sort the features
    while current_features:
        current_layer = set()
        for f in current_features:
            if isinstance(f, Feature):
                # Anchor feature doesn't have upstream
                current_features.remove(f)
                current_layer.add(f)
            elif isinstance(f, DerivedFeature):
                pending = False
                # If any of the input features is still left, it shall wait
                for input in f.input_features:
                    if input in current_features:
                        pending = True
                        break
                if not pending:
                    current_features.remove(f)
                    current_layer.add(f)
        layers.append(current_layer)
        
    identifiers = set()
    stages = []
    for l in layers:
        t = []
        for f in l:
            expr = ""
            if isinstance(f.transform, ExpressionTransformation):
                expr = f.transform.expr
            elif isinstance(f.transform, str):
                expr = f.transform
            elif isinstance(f.transform, WindowAggTransformation):
                raise NotImplementedError(f"Feature {f.name} is using WindowAggTransformation, which is not supported")
            t.append(f"{f.name} = {expr}")
            unsupported_func = ""
            try:
                for id in get_identifiers(expr):
                    if id not in feature_names:
                        if re.match("^[A-Za-z_][A-Za-z0-9_]*$", id):
                            identifiers.add(id)
            except NotImplementedError as e:
                unsupported_func = f"{e}"
            if unsupported_func:
                raise NotImplementedError(f"Feature {f.name} uses unsupported function {unsupported_func}")
        stages.append(f'| project {", ".join(t)}')
    stages.append(f'| project-keep {", ".join([f.name for f in features])}', )
    schema = f'({", ".join(identifiers)})'
    return "\n".join([f'{name}{schema}', "\n".join(stages), ";"])
